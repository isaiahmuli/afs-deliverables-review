package transfers.rates;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import transfers.core.RateSource;
import transfers.core.UnknownCurrencyException;

/**
 * The night's bank rates: a thread-safe Singleton. The bank charges per fetch, so the
 * table is fetched once per process (one night), however many batches ask for it at once.
 *
 * <p>Double-checked locking on a volatile field: after the first fetch, callers read the
 * field without locking; only the first callers race for the lock, and only one fetches.
 */
public final class BankRates implements RateSource {
    private static final Logger log = LoggerFactory.getLogger(BankRates.class);
    private static final AtomicInteger fetches = new AtomicInteger();
    private static volatile BankRates instance;

    private final Path file;
    private final Map<String, BigDecimal> rates;

    private BankRates(Path file) {
        this.file = file;
        this.rates = Map.copyOf(fetch(file));
    }

    public static BankRates getInstance(Path file) {
        BankRates current = instance;
        if (current == null) {
            synchronized (BankRates.class) {
                current = instance;
                if (current == null) {
                    current = new BankRates(file);
                    instance = current;
                }
            }
        }
        if (!current.file.toAbsolutePath().normalize().equals(file.toAbsolutePath().normalize())) {
            throw new IllegalStateException("Rates already fetched tonight from " + current.file);
        }
        return current;
    }

    /** How many times the bank was asked for rates in this process. */
    public static int fetchCount() {
        return fetches.get();
    }

    /** Tests only: forget tonight's table so a test can start a fresh "night". */
    static synchronized void resetForTesting() {
        instance = null;
        fetches.set(0);
    }

    @Override
    public BigDecimal unitsPerUsd(String currency) {
        BigDecimal rate = rates.get(currency);
        if (rate == null) {
            throw new UnknownCurrencyException(currency);
        }
        return rate;
    }

    private static Map<String, BigDecimal> fetch(Path file) {
        log.info("Fetching bank rates, fetch {}", fetches.incrementAndGet());
        try {
            Thread.sleep(40); // simulates the bank download
            List<String> lines = Files.readAllLines(file);
            Map<String, BigDecimal> table = new HashMap<>();
            // Every currency in the bank's file. The original kept a fixed list of four,
            // which is why MXN had no rate (see docs/root-cause-mexico.md).
            for (String line : lines.subList(1, lines.size())) {
                if (line.isBlank()) continue;
                String[] columns = line.split(",");
                table.put(columns[0].trim(), new BigDecimal(columns[1].trim()));
            }
            return table;
        } catch (IOException e) {
            throw new UncheckedIOException("Bank rates unavailable: " + file, e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Bank rate fetch interrupted", e);
        }
    }
}
