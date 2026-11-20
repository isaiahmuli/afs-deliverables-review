package boutique;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;

public class Checkout implements AutoCloseable {
    public static final int DEFAULT_LOW_STOCK_THRESHOLD = 5;

    private final List<Product> products;
    private final RecordWriter<Order> manifest;
    private final RecordWriter<AuditEvent> audit;
    private final int lowStockThreshold;

    /** Original constructor: no audit log, low-stock threshold 5. */
    public Checkout(List<Product> products, RecordWriter<Order> manifest) {
        this(products, manifest, event -> { }, DEFAULT_LOW_STOCK_THRESHOLD);
    }

    public Checkout(List<Product> products, RecordWriter<Order> manifest, RecordWriter<AuditEvent> audit,
                    int lowStockThreshold) {
        if (lowStockThreshold < 0) {
            throw new IllegalArgumentException("Low-stock threshold must not be negative: " + lowStockThreshold);
        }
        this.products = List.copyOf(products);
        this.manifest = Objects.requireNonNull(manifest, "manifest");
        this.audit = Objects.requireNonNull(audit, "audit");
        this.lowStockThreshold = lowStockThreshold;
    }

    /** The product with this code, or empty if the store does not sell it. */
    public Optional<Product> findProduct(String code) {
        return products.stream()
                .filter(product -> product.code().equals(code))
                .findFirst();
    }

    /**
     * Reserves the units and queues the order for the manifest and the audit log.
     * Returns without waiting for either file.
     *
     * @return false, changing nothing and writing nothing, for an unknown code or a zero,
     *     negative, or unavailable quantity
     */
    public boolean checkout(String orderId, String code, int quantity) throws IOException {
        Optional<Order> reserved = findProduct(code).flatMap(product -> product.reserve(orderId, quantity));
        if (reserved.isEmpty()) {
            return false;
        }
        Order order = reserved.orElseThrow();
        manifest.append(order);
        audit.append(AuditEvent.Reservation.of(order));
        return true;
    }

    /** Changes the price and records it in the audit log. Returns false for an unknown code. */
    public boolean changePrice(String code, long priceCents) throws IOException {
        Optional<Product> product = findProduct(code);
        if (product.isEmpty()) {
            return false;
        }
        long oldPrice = product.orElseThrow().changePrice(priceCents);
        audit.append(new AuditEvent.PriceChange(Instant.now(), code, oldPrice, priceCents));
        return true;
    }

    /** Products in catalog order that match the filter. */
    public List<Product> productsMatching(Predicate<? super Product> filter) {
        return products.stream().filter(filter).toList();
    }

    /** Products with fewer units than the store's threshold, in catalog order. */
    public List<Product> lowStock() {
        return productsMatching(product -> product.stock() < lowStockThreshold);
    }

    /** The original low-stock loop, kept unchanged so a test can compare it with lowStock(). */
    List<Product> originalLowStockLoop() {
        List<Product> result = new ArrayList<>();
        for (Product product : products) {
            if (product.stock() < 5) result.add(product);
        }
        return result;
    }

    /** Waits for every pending manifest and audit line, then closes both files. */
    @Override
    public void close() throws IOException {
        try {
            manifest.close();
        } finally {
            audit.close();
        }
    }
}
