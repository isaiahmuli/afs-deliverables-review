package boutique;

import java.time.Instant;

/** A reserved order. Immutable, so it is safe to hand to the writer thread. */
public record Order(String id, String productCode, int quantity, long unitPriceCents, Instant placedAt)
        implements CsvRecord {

    public Order(String id, String productCode, int quantity, long unitPriceCents) {
        this(id, productCode, quantity, unitPriceCents, Instant.now());
    }

    /** The manifest line: {@code order-1,JACKET,1,4999}. */
    @Override
    public String toCsvLine() {
        return String.join(",", id, productCode, Integer.toString(quantity), Long.toString(unitPriceCents));
    }
}
