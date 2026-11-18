package boutique;

import java.time.Instant;

/** One line in the owner's audit log. */
public sealed interface AuditEvent extends CsvRecord {

    /** {@code 2026-11-18T10:00:00Z,RESERVED,order-1,JACKET,1,4999} */
    record Reservation(Instant at, String orderId, String productCode, int quantity, long unitPriceCents)
            implements AuditEvent {
        static Reservation of(Order order) {
            return new Reservation(order.placedAt(), order.id(), order.productCode(), order.quantity(),
                    order.unitPriceCents());
        }

        @Override
        public String toCsvLine() {
            return String.join(",", at.toString(), "RESERVED", orderId, productCode,
                    Integer.toString(quantity), Long.toString(unitPriceCents));
        }
    }

    /** {@code 2026-11-18T10:00:00Z,PRICE_CHANGED,JACKET,4999,4499} (old price, then new price) */
    record PriceChange(Instant at, String productCode, long oldPriceCents, long newPriceCents)
            implements AuditEvent {
        @Override
        public String toCsvLine() {
            return String.join(",", at.toString(), "PRICE_CHANGED", productCode,
                    Long.toString(oldPriceCents), Long.toString(newPriceCents));
        }
    }
}
