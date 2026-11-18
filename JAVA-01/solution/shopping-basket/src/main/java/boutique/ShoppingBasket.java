package boutique;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class ShoppingBasket {
    /** Keyed lookup by normalized code; one entry per product. */
    private final Map<ProductCode, Product> catalog = new HashMap<>();
    /** Keyed by product so repeat adds merge; LinkedHashMap keeps first-added order. */
    private final Map<ProductCode, BasketLine> lines = new LinkedHashMap<>();
    private Discount discount;
    private Delivery delivery = Delivery.STANDARD;

    public ShoppingBasket(List<Product> products) {
        for (Product product : products) {
            if (catalog.putIfAbsent(product.key(), product) != null) {
                throw new IllegalArgumentException("Duplicate product code in catalog: " + product.code());
            }
        }
    }

    /**
     * Adds a quantity of a product. Adding a product already in the basket increases its
     * line's quantity; the line keeps its original position.
     *
     * @throws UnknownProductException if no catalog product has this code (recoverable)
     * @throws IllegalArgumentException if the quantity is not positive (caller error)
     */
    public void add(String productCode, int quantity) throws UnknownProductException {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be positive: " + quantity);
        }
        ProductCode key = ProductCode.of(productCode);
        Product product = catalog.get(key);
        if (product == null) {
            throw new UnknownProductException(productCode);
        }
        lines.merge(key, BasketLine.of(product, quantity), (existing, added) -> existing.plus(added.quantity()));
    }

    public void setDiscount(Discount discount) {
        this.discount = Objects.requireNonNull(discount, "discount");
    }

    public void setDelivery(Delivery delivery) {
        this.delivery = Objects.requireNonNull(delivery, "delivery");
    }

    /** An unmodifiable snapshot of the lines, in the order each product was first added. */
    public List<BasketLine> lines() {
        return List.copyOf(lines.values());
    }

    public long subtotalCents() {
        long subtotal = 0;
        for (BasketLine line : lines.values()) {
            subtotal = Math.addExact(subtotal, line.totalCents());
        }
        return subtotal;
    }

    public long discountedSubtotalCents() {
        long subtotal = subtotalCents();
        return discount == null ? subtotal : discount.apply(subtotal);
    }

    /** Standard $5.00, express $12.00, pickup free. No default branch: a new option must be priced here. */
    public long deliveryCents() {
        return switch (delivery) {
            case Delivery.Standard() -> 500;
            case Delivery.Express() -> 1200;
            case Delivery.Pickup() -> 0;
        };
    }

    public long checkoutTotalCents() {
        return Math.addExact(discountedSubtotalCents(), deliveryCents());
    }
}
