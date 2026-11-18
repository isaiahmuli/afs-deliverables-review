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
        Path manifest = Files.createTempDirectory(output, "sale-").resolve("manifest.csv");
        var jacket = new Product("JACKET", 4999, 60);
        var accepted = new AtomicInteger();
        var errors = new AtomicInteger();
        var ready = new CountDownLatch(200);
        var start = new CountDownLatch(1);
        Thread[] shoppers = new Thread[200];
        try (var checkout = new Checkout(List.of(jacket), new FileManifestWriter(manifest))) {
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
        }
        System.out.println("Jackets available: 60");
        System.out.println("Shoppers: 200");
        System.out.println("Reservations accepted: " + accepted.get());
        System.out.println("Jackets left: " + jacket.stock());
        System.out.println("Checkout errors: " + errors.get());
        System.out.println("Manifest lines after close: " + Files.readAllLines(manifest).size());
        System.out.println("Manifest: " + manifest.toAbsolutePath());
    }
}
