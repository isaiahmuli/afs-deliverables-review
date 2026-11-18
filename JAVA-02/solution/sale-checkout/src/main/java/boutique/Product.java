package boutique;

import java.util.Optional;

/**
 * A product whose stock and price can change while shoppers check out. Every read and
 * change holds this product's lock, so a reservation checks and takes stock in one step.
 */
public class Product {
    private final String code;
    private long priceCents;
    private int stock;

    public Product(String code, long priceCents, int stock) {
        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException("Product code must not be blank");
        }
        if (priceCents < 0 || stock < 0) {
            throw new IllegalArgumentException("Price and stock must not be negative");
        }
        this.code = code;
        this.priceCents = priceCents;
        this.stock = stock;
    }

    public String code() { return code; }
    public synchronized long priceCents() { return priceCents; }
    public synchronized int stock() { return stock; }
    public synchronized boolean isSoldOut() { return stock == 0; }

    /**
     * Takes {@code quantity} units if they are all available, at today's price.
     * Returns empty, changing nothing, for a zero, negative, or unavailable quantity.
     */
    synchronized Optional<Order> reserve(String orderId, int quantity) {
        if (quantity <= 0 || quantity > stock) {
            return Optional.empty();
        }
        stock -= quantity;
        return Optional.of(new Order(orderId, code, quantity, priceCents));
    }

    /** Sets the price and returns the previous one. */
    synchronized long changePrice(long newPriceCents) {
        if (newPriceCents < 0) {
            throw new IllegalArgumentException("Price must not be negative: " + newPriceCents);
        }
        long old = priceCents;
        priceCents = newPriceCents;
        return old;
    }
}
