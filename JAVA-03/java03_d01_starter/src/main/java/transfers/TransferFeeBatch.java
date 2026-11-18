package transfers;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class TransferFeeBatch {
    private static final Logger log = LoggerFactory.getLogger(TransferFeeBatch.class);
    private final Path ratesFile;
    private final MockSmsService sms = new MockSmsService();

    public TransferFeeBatch(Path ratesFile) {
        this.ratesFile = ratesFile;
    }

    public void run(Path input, Path output) throws Exception {
        List<Transfer> transfers = Files.readAllLines(input).stream().skip(1)
                .filter(line -> !line.isBlank()).map(Transfer::parse).toList();
        List<Future<List<Priced>>> pending = new ArrayList<>();
        List<Priced> priced = new ArrayList<>();
        int chunkSize = (transfers.size() + 3) / 4;
        try (var pool = Executors.newFixedThreadPool(4)) {
            for (int batch = 0; batch < 4; batch++) {
                int from = Math.min(batch * chunkSize, transfers.size());
                int to = Math.min(from + chunkSize, transfers.size());
                pending.add(pool.submit(() -> process(transfers.subList(from, to))));
            }
            for (Future<List<Priced>> future : pending) priced.addAll(future.get());
        }
        priced.sort(Comparator.comparing(result -> result.transfer().id()));
        List<String> lines = new ArrayList<>();
        lines.add("id,type,amount_usd,fee_usd,recipient_amount,recipient_currency");
        for (Priced result : priced) lines.add(result.csv());
        Files.createDirectories(output.toAbsolutePath().getParent());
        Files.write(output, lines);
        log.info("Read={} priced={} refused={} rate fetches={} SMS receipts={}",
                transfers.size(), priced.size(), transfers.size() - priced.size(),
                RateTable.fetches.get(), sms.sent.get());
        log.info("Fees written to {}", output.toAbsolutePath());
    }

    private List<Priced> process(List<Transfer> transfers) throws InterruptedException {
        List<Priced> results = new ArrayList<>();
        for (Transfer transfer : transfers) {
            try {
                Priced priced = price(transfer);
                String messageId = sms.send(transfer.sender(), "Transfer " + transfer.id()
                        + ": amount USD " + transfer.amount() + ", fee USD " + priced.fee()
                        + ", recipient " + priced.recipientAmount() + " " + transfer.currency());
                log.debug("Receipt {} for transfer {}", messageId, transfer.id());
                if (transfer.amount().compareTo(new BigDecimal("5000")) > 0) {
                    System.out.println("REVIEW " + transfer.id() + " USD " + transfer.amount());
                }
                results.add(priced);
            } catch (IllegalArgumentException e) {
                log.warn("Refused transfer {} type={}: {}", transfer.id(), transfer.type(), e.getMessage());
            } catch (IllegalStateException e) {
                log.error("Transfer {} type={} failed", transfer.id(), transfer.type(), e);
            }
        }
        return results;
    }

    public Priced price(Transfer transfer) {
        BigDecimal fee = switch (transfer.type()) {
            case "friend" -> BigDecimal.ZERO;
            case "bank" -> new BigDecimal("0.50");
            case "international" -> transfer.amount().multiply(new BigDecimal("0.02"))
                    .max(new BigDecimal("3.00"));
            default -> throw new IllegalArgumentException("Unsupported transfer type: " + transfer.type());
        };
        try {
            BigDecimal recipient = transfer.amount();
            if (transfer.type().equals("international")) {
                recipient = convert(transfer.amount(), RateTable.getInstance(ratesFile).rate(transfer.currency()));
            }
            return new Priced(transfer, cents(fee), cents(recipient));
        } catch (RuntimeException e) {
            throw new IllegalStateException("Could not price transfer " + transfer.id(), e);
        }
    }

    private static BigDecimal convert(BigDecimal amount, BigDecimal rate) {
        return amount.multiply(rate);
    }

    private static BigDecimal cents(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP);
    }

    public record Transfer(String id, String type, String sender, BigDecimal amount, String currency) {
        static Transfer parse(String line) {
            String[] columns = line.split(",", -1);
            return new Transfer(columns[0], columns[1], columns[2], new BigDecimal(columns[3]), columns[4]);
        }
    }

    public record Priced(Transfer transfer, BigDecimal fee, BigDecimal recipientAmount) {
        String csv() {
            return String.join(",", transfer.id(), transfer.type(), transfer.amount().toPlainString(),
                    fee.toPlainString(), recipientAmount.toPlainString(), transfer.currency());
        }
    }

    static final class RateTable {
        private static RateTable instance;
        private static final AtomicInteger fetches = new AtomicInteger();
        private final Map<String, BigDecimal> rates = new HashMap<>();

        private RateTable(Path file) {
            log.info("Fetching bank rates, fetch {}", fetches.incrementAndGet());
            try {
                Thread.sleep(40);
                List<String> lines = Files.readAllLines(file);
                for (String line : lines.subList(1, lines.size())) {
                    String[] columns = line.split(",");
                    if (Set.of("EUR", "GBP", "CAD", "INR").contains(columns[0])) {
                        rates.put(columns[0], new BigDecimal(columns[1]));
                    }
                }
            } catch (IOException e) {
                throw new IllegalStateException("Bank rates unavailable", e);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("Bank rate fetch interrupted", e);
            }
        }

        static RateTable getInstance(Path file) {
            if (instance == null) instance = new RateTable(file);
            return instance;
        }

        BigDecimal rate(String currency) { return rates.get(currency); }
    }

    static final class MockSmsService {
        private final AtomicInteger sent = new AtomicInteger();

        String send(String sender, String body) throws InterruptedException {
            Thread.sleep(5);
            String id = "sms-" + sent.incrementAndGet();
            log.debug("SMS {} to {}: {}", id, sender, body);
            return id;
        }
    }
}
