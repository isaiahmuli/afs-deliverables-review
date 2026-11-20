package boutique;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

public class Main {
    public static void main(String[] args) throws Exception {
        Path output = args.length == 0 ? Path.of("build", "sale-output") : Path.of(args[0]);
        Files.createDirectories(output);
        Path run = Files.createTempDirectory(output, "sale-");
        Path manifest = run.resolve("manifest.csv");
        Path auditLog = run.resolve("audit.csv");
        var jacket = new Product("JACKET", 4999, 60);
        var accepted = new AtomicInteger();
        var errors = new AtomicInteger();
        var ready = new CountDownLatch(200);
        var start = new CountDownLatch(1);
        Thread[] shoppers = new Thread[200];
        try (var checkout = new Checkout(List.of(jacket), new FileManifestWriter(manifest),
                new AsyncFileWriter<AuditEvent>(auditLog, CsvRecord::toCsvLine), Checkout.DEFAULT_LOW_STOCK_THRESHOLD)) {
            for (int i = 0; i < shoppers.length; i++) {
                String id = "order-" + (i + 1);
                shoppers[i] = new Thread(() -> {
                    ready.countDown();
                    try {
                        start.await();
                        if (checkout.checkout(id, "JACKET", 1)) accepted.incrementAndGet();
                    } catch (Exception e) {
                        errors.incrementAndGet();
                        System.err.println(id + ": " + e.getMessage());
                    }
                });
                shoppers[i].start();
            }
            ready.await();
            start.countDown();
            for (Thread shopper : shoppers) shopper.join();
            checkout.changePrice("JACKET", 5499); // after the sale; recorded in the audit log
        }
        System.out.println("Jackets available: 60");
        System.out.println("Shoppers: 200");
        System.out.println("Reservations accepted: " + accepted.get());
        System.out.println("Jackets left: " + jacket.stock());
        System.out.println("Checkout errors: " + errors.get());
        System.out.println("Manifest lines after close: " + Files.readAllLines(manifest).size());
        System.out.println("Audit lines after close: " + Files.readAllLines(auditLog).size());
        System.out.println("Manifest: " + manifest.toAbsolutePath());
        System.out.println("Audit log: " + auditLog.toAbsolutePath());
    }
}
