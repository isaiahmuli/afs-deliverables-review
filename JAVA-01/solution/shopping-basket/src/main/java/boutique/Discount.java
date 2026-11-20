package boutique;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * A discount on the items, applied before delivery. Each kind implements
 * {@link #apply(long)}; the basket calls it without checking which kind it has.
 *
 * <p>Deliberately not sealed: a new discount is one new class that implements this
 * interface, with no change to the basket or to the existing discounts.
 */
public interface Discount {

    /** Returns the items subtotal after this discount. Never below zero. */
    long apply(long subtotalCents);

    /** Percent off the items, rounded to the nearest cent, half up. */
    record Percentage(int percent) implements Discount {
        public Percentage {
            if (percent < 0 || percent > 100) {
                throw new IllegalArgumentException("Percent must be 0 to 100: " + percent);
            }
        }

        @Override
        public long apply(long subtotalCents) {
            long discountCents = BigDecimal.valueOf(subtotalCents)
                    .multiply(BigDecimal.valueOf(percent))
                    .divide(BigDecimal.valueOf(100), 0, RoundingMode.HALF_UP)
                    .longValueExact();
            return Math.max(0, subtotalCents - discountCents);
        }
    }

    /** A fixed amount off the items; never takes them below zero. */
    record FixedAmount(long cents) implements Discount {
        public FixedAmount {
            if (cents < 0) {
                throw new IllegalArgumentException("Discount must not be negative: " + cents);
            }
        }

        @Override
        public long apply(long subtotalCents) {
            return Math.max(0, subtotalCents - cents);
        }
    }
}
