package boutique;

import java.time.Instant;

public class Order {
    private final String id;
    private final String productCode;
    private final int quantity;
    private final long unitPriceCents;
    private final Instant placedAt;

    public Order(String id, String productCode, int quantity, long unitPriceCents) {
        this.id = id;
        this.productCode = productCode;
        this.quantity = quantity;
        this.unitPriceCents = unitPriceCents;
        this.placedAt = Instant.now();
    }

    public String id() { return id; }
    public String productCode() { return productCode; }
    public int quantity() { return quantity; }
    public long unitPriceCents() { return unitPriceCents; }
    public Instant placedAt() { return placedAt; }
}
