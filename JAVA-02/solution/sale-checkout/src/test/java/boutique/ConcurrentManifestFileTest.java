package boutique;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.io.TempDir;

/**
 * The sale with the real files: 200 shoppers on an executor, 60 jackets. Checks the
 * manifest's and the audit log's contents, line by line, not only the counts.
 */
class ConcurrentManifestFileTest {
    @TempDir Path directory;

    @RepeatedTest(10)
    void everyAcceptedOrderIsOneCompleteManifestLineAndRefusedOrdersAreAbsent() throws Exception {
        Path manifest = directory.resolve("manifest.csv");
        Path audit = directory.resolve("audit.csv");
        var jacket = new Product("JACKET", 4999, 60);
        var accepted = new ConcurrentLinkedQueue<String>();
        var refused = new ConcurrentLinkedQueue<String>();
        var start = new CountDownLatch(1);

        try (var checkout = new Checkout(List.of(jacket), new FileManifestWriter(manifest),
                     new AsyncFileWriter<AuditEvent>(audit, CsvRecord::toCsvLine), 5);
             var shoppers = Executors.newFixedThreadPool(200)) {
            List<Future<Boolean>> results = new ArrayList<>();
            for (int i = 1; i <= 200; i++) {
                String id = "order-" + i;
                results.add(shoppers.submit(() -> {
                    start.await();
                    boolean ok = checkout.checkout(id, "JACKET", 1);
                    (ok ? accepted : refused).add(id);
                    return ok;
                }));
            }
            start.countDown();
            for (Future<Boolean> result : results) {
                result.get(); // rethrows any checkout exception
            }
        } // executor closed (shut down and awaited), then checkout closed: all lines written

        assertThat(accepted).hasSize(60);
        assertThat(refused).hasSize(140);
        assertThat(jacket.stock()).isZero();

        List<String> lines = Files.readAllLines(manifest);
        assertThat(lines).hasSize(60).doesNotHaveDuplicates()
                .allMatch(line -> line.matches("order-\\d+,JACKET,1,4999"), "a complete manifest line");
        assertThat(lines).extracting(line -> line.split(",")[0])
                .containsExactlyInAnyOrderElementsOf(accepted)
                .doesNotContainAnyElementsOf(refused);

        List<String> auditLines = Files.readAllLines(audit);
        assertThat(auditLines).hasSize(60)
                .allMatch(line -> line.matches("\\S+,RESERVED,order-\\d+,JACKET,1,4999"), "a complete audit line");
        assertThat(auditLines).extracting(line -> line.split(",")[2])
                .containsExactlyInAnyOrderElementsOf(accepted);
    }
}
