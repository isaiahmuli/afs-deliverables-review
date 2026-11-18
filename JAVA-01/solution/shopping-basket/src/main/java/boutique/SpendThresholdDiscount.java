package boutique;

/**
 * New discount: "spend X, save Y". When the items subtotal reaches the threshold, a fixed
 * amount comes off the items; below the threshold, nothing comes off.
 *
 * <p>Example, $10.00 off when items reach $50.00:
 * <ul>
 *   <li>items $68.00 → $58.00</li>
 *   <li>items exactly $50.00 → $40.00 (the threshold counts)</li>
 *   <li>items $49.99 → $49.99 (no discount)</li>
 * </ul>
 * The amount off may not exceed the threshold, so the items never go below zero.
 *
 * <p>Added as one new class: neither the basket nor the existing discounts changed.
 */
public record SpendThresholdDiscount(long thresholdCents, long amountOffCents) implements Discount {
    public SpendThresholdDiscount {
        if (thresholdCents <= 0) {
            throw new IllegalArgumentException("Threshold must be positive: " + thresholdCents);
        }
        if (amountOffCents <= 0 || amountOffCents > thresholdCents) {
            throw new IllegalArgumentException(
                    "Amount off must be positive and not above the threshold: " + amountOffCents);
        }
    }

    @Override
    public long apply(long subtotalCents) {
        return subtotalCents >= thresholdCents ? subtotalCents - amountOffCents : subtotalCents;
    }
}
