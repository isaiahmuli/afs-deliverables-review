package transfers.batch;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import transfers.core.FeeCalculator;
import transfers.core.Quote;
import transfers.core.Transfer;
import transfers.core.UnknownCurrencyException;
import transfers.core.UnsupportedTransferTypeException;
import transfers.events.PricingEvents;

/**
 * The nightly batch: reads transfers, prices them in parallel groups, tells listeners
 * (receipts, review flags) about each priced transfer, and writes the fee file.
 * It holds no fee rules itself.
 */
public final class TransferFeeBatch {
    private static final Logger log = LoggerFactory.getLogger(TransferFeeBatch.class);

    private final FeeCalculator calculator;
    private final PricingEvents events;
    private final int parallelGroups;

    public TransferFeeBatch(FeeCalculator calculator, PricingEvents events, int parallelGroups) {
        if (parallelGroups < 1) {
            throw new IllegalArgumentException("Need at least one group: " + parallelGroups);
        }
        this.calculator = Objects.requireNonNull(calculator, "calculator");
        this.events = Objects.requireNonNull(events, "events");
        this.parallelGroups = parallelGroups;
    }

    public BatchResult run(Path input, Path output) throws IOException, InterruptedException {
        List<Transfer> transfers = Files.readAllLines(input).stream().skip(1)
                .filter(line -> !line.isBlank()).map(TransferCsv::parse).toList();
        List<Quote> quotes = new ArrayList<>();
        int groupSize = Math.max(1, (transfers.size() + parallelGroups - 1) / parallelGroups);
        try (var pool = Executors.newFixedThreadPool(parallelGroups)) {
            List<Future<List<Quote>>> pending = new ArrayList<>();
            for (int from = 0; from < transfers.size(); from += groupSize) {
                List<Transfer> group = transfers.subList(from, Math.min(from + groupSize, transfers.size()));
                pending.add(pool.submit(() -> process(group)));
            }
            for (Future<List<Quote>> future : pending) {
                quotes.addAll(future.get());
            }
        } catch (ExecutionException e) {
            throw new IllegalStateException("A batch group failed", e.getCause());
        }
        quotes.sort(Comparator.comparing(quote -> quote.transfer().id()));
        List<String> lines = new ArrayList<>();
        lines.add(TransferCsv.OUTPUT_HEADER);
        quotes.forEach(quote -> lines.add(TransferCsv.format(quote)));
        Files.createDirectories(output.toAbsolutePath().getParent());
        Files.write(output, lines);
        log.info("Fees written to {}", output.toAbsolutePath());
        return new BatchResult(transfers.size(), quotes.size(), transfers.size() - quotes.size());
    }

    private List<Quote> process(List<Transfer> group) {
        List<Quote> priced = new ArrayList<>();
        for (Transfer transfer : group) {
            try {
                Quote quote = calculator.price(transfer);
                events.publish(quote);
                priced.add(quote);
            } catch (UnsupportedTransferTypeException e) {
                log.warn("Refused transfer {} type={}: unsupported type", e.transferId(), e.type());
            } catch (UnknownCurrencyException e) {
                log.error("Transfer {} type={} failed: {}", transfer.id(), transfer.type(), e.getMessage());
            }
        }
        return priced;
    }
}
