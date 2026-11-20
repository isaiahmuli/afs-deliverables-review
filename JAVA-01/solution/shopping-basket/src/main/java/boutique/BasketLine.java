package boutique;

/**
 * One basket line. A record, so it is immutable: a caller holding a line cannot change
 * its quantity. The basket replaces the line when the shopper adds more.
 */
public record BasketLine(String productCode, String productName, long unitPriceCents, int quantity) {
    public BasketLine {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be positive: " + quantity);
        }
        if (unitPriceCents < 0) {
            throw new IllegalArgumentException("Price must not be negative: " + unitPriceCents);
        }
    }

    static BasketLine of(Product product, int quantity) {
        return new BasketLine(product.code(), product.name(), product.priceCents(), quantity);
    }

    BasketLine plus(int extraQuantity) {
        return new BasketLine(productCode, productName, unitPriceCents, Math.addExact(quantity, extraQuantity));
    }

    public long totalCents() {
        return Math.multiplyExact(unitPriceCents, quantity);
    }
}
