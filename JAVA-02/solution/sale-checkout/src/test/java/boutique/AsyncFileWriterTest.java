package boutique;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class AsyncFileWriterTest {
    @TempDir Path directory;

    @Test
    void oneWriterClassServesTwoRecordTypesWithOneSupertypeFormatter() throws Exception {
        // Function<CsvRecord, String> is accepted as Function<? super Order, String> and
        // Function<? super AuditEvent, String>: the wildcard the writer relies on.
        Function<CsvRecord, String> asCsv = CsvRecord::toCsvLine;
        Path manifest = directory.resolve("manifest.csv");
        Path audit = directory.resolve("audit.csv");
        var at = Instant.parse("2026-11-18T10:00:00Z");

        try (var orders = new AsyncFileWriter<Order>(manifest, asCsv);
             var events = new AsyncFileWriter<AuditEvent>(audit, asCsv)) {
            orders.append(new Order("order-1", "JACKET", 2, 4999));
            events.append(new AuditEvent.Reservation(at, "order-1", "JACKET", 2, 4999));
            events.append(new AuditEvent.PriceChange(at, "JACKET", 4999, 4499));
        }

        assertThat(Files.readAllLines(manifest)).containsExactly("order-1,JACKET,2,4999");
        assertThat(Files.readAllLines(audit)).containsExactly(
                "2026-11-18T10:00:00Z,RESERVED,order-1,JACKET,2,4999",
                "2026-11-18T10:00:00Z,PRICE_CHANGED,JACKET,4999,4499");
    }

    @Test
    void theFormatterCanBeALambda() throws Exception {
        Path path = directory.resolve("ids.txt");
        try (var writer = new AsyncFileWriter<Order>(path, order -> order.id())) {
            writer.append(new Order("order-9", "JACKET", 1, 4999));
        }
        assertThat(Files.readAllLines(path)).containsExactly("order-9");
    }

    @Test
    void checkoutReturnsBeforeTheManifestWriteFinishes() throws Exception {
        Path path = directory.resolve("manifest.csv");
        var writeMayFinish = new CountDownLatch(1);
        var writeStarted = new CountDownLatch(1);
        Function<Order, String> slowFormatter = order -> {
            writeStarted.countDown();
            awaitQuietly(writeMayFinish);
            return order.toCsvLine();
        };
        var writer = new AsyncFileWriter<Order>(path, slowFormatter);
        var checkout = new Checkout(List.of(new Product("JACKET", 4999, 60)), writer);

        // Checkout runs on another thread so the test cannot hang; it must finish within
        // 2 s even though the write it queued is blocked until the latch opens.
        var result = CompletableFuture.supplyAsync(() -> {
            try {
                return checkout.checkout("order-1", "JACKET", 1);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        });

        assertThat(result).succeedsWithin(Duration.ofSeconds(2)).isEqualTo(true);
        assertThat(writeStarted.await(2, TimeUnit.SECONDS)).isTrue();
        assertThat(Files.readAllLines(path)).as("the write is still in progress").isEmpty();

        writeMayFinish.countDown();
        checkout.close();
        assertThat(Files.readAllLines(path)).containsExactly("order-1,JACKET,1,4999");
    }

    @Test
    void closeWritesEveryPendingLine() throws Exception {
        Path path = directory.resolve("manifest.csv");
        Function<Order, String> slowFormatter = order -> {
            sleepQuietly(1);
            return order.toCsvLine();
        };
        var writer = new AsyncFileWriter<Order>(path, slowFormatter);
        for (int i = 1; i <= 300; i++) {
            writer.append(new Order("order-" + i, "JACKET", 1, 4999));
        }
        assertThat(Files.readAllLines(path)).as("most lines are still queued").hasSizeLessThan(300);

        writer.close();

        assertThat(Files.readAllLines(path)).containsExactlyElementsOf(
                IntStream.rangeClosed(1, 300).mapToObj(i -> "order-" + i + ",JACKET,1,4999").toList());
    }

    @Test
    void appendAfterCloseIsRejected() throws Exception {
        var writer = new FileManifestWriter(directory.resolve("manifest.csv"));
        writer.close();
        assertThatThrownBy(() -> writer.append(new Order("order-1", "JACKET", 1, 4999)))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void aFailedWriteIsReportedOnClose() throws Exception {
        var writer = new AsyncFileWriter<Order>(directory.resolve("manifest.csv"), order -> {
            throw new IllegalStateException("cannot format");
        });
        writer.append(new Order("order-1", "JACKET", 1, 4999));
        assertThatThrownBy(writer::close).isInstanceOf(IOException.class).hasRootCauseMessage("cannot format");
    }

    @Test
    void aRecordMustFormatAsOneLine() throws Exception {
        Path path = directory.resolve("manifest.csv");
        var writer = new AsyncFileWriter<Order>(path, order -> order.id() + "\n" + order.productCode());
        writer.append(new Order("order-1", "JACKET", 1, 4999));
        assertThatThrownBy(writer::close).isInstanceOf(IOException.class).hasMessageContaining("one line");
        assertThat(Files.readAllLines(path)).isEmpty();
    }

    @Test
    void appendAfterAFailedWriteReportsTheFailure() throws Exception {
        var writer = new AsyncFileWriter<Order>(directory.resolve("manifest.csv"), order -> {
            throw new IllegalStateException("cannot format");
        });
        writer.append(new Order("order-1", "JACKET", 1, 4999));
        // The first write fails in the background; the next append reports it.
        Thread.sleep(200);
        assertThatThrownBy(() -> writer.append(new Order("order-2", "JACKET", 1, 4999)))
                .isInstanceOf(IOException.class);
        assertThatThrownBy(writer::close).isInstanceOf(IOException.class);
    }

    private static void awaitQuietly(CountDownLatch latch) {
        try {
            latch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private static void sleepQuietly(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
