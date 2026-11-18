package transfers.rates;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import transfers.core.UnknownCurrencyException;

class BankRatesSingletonTest {
    private static final Path RATES = Path.of("bank-rates.csv");

    @BeforeEach
    @AfterEach
    void newNight() {
        BankRates.resetForTesting();
    }

    @RepeatedTest(20)
    void manyThreadsAtOnceGetOneInstanceAndOneFetch() throws Exception {
        int threads = 32;
        var start = new CountDownLatch(1);
        Set<BankRates> seen = ConcurrentHashMap.newKeySet();
        try (var pool = Executors.newFixedThreadPool(threads)) {
            List<Future<?>> calls = new ArrayList<>();
            for (int i = 0; i < threads; i++) {
                calls.add(pool.submit(() -> {
                    start.await();
                    seen.add(BankRates.getInstance(RATES));
                    return null;
                }));
            }
            start.countDown();
            for (Future<?> call : calls) call.get();
        }
        assertEquals(1, seen.size(), "distinct instances");
        assertEquals(1, BankRates.fetchCount(), "bank fetches");
    }

    @Test
    void laterCallsReuseTonightsTable() {
        var first = BankRates.getInstance(RATES);
        assertSame(first, BankRates.getInstance(Path.of("./bank-rates.csv")));
        assertEquals(1, BankRates.fetchCount());
    }

    @Test
    void everyCurrencyInTheFileIsLoadedIncludingMexico() {
        var rates = BankRates.getInstance(RATES);
        for (String currency : List.of("EUR", "GBP", "CAD", "INR", "MXN")) {
            rates.unitsPerUsd(currency);
        }
        assertEquals(new BigDecimal("17.25"), rates.unitsPerUsd("MXN"));
        assertThrows(UnknownCurrencyException.class, () -> rates.unitsPerUsd("JPY"));
    }

    @Test
    void aSecondRatesFileInOneNightIsRejected() {
        BankRates.getInstance(RATES);
        assertThrows(IllegalStateException.class, () -> BankRates.getInstance(Path.of("other-rates.csv")));
    }

    @Test
    void aMissingFileFailsClearly() {
        assertThrows(java.io.UncheckedIOException.class, () -> BankRates.getInstance(Path.of("no-such-rates.csv")));
    }
}
