package com.example.stock.domain;

/**
 * A removal would make stock negative. Checked on purpose: the caller must decide what to tell
 * the person. Spring does not roll back on checked exceptions unless told to (rollbackFor).
 */
public class InsufficientStockException extends Exception {

    private final String productName;
    private final int available;
    private final int requested;

    public InsufficientStockException(String productName, int available, int requested) {
        super("Cannot remove %d of %s: only %d in stock.".formatted(requested, productName, available));
        this.productName = productName;
        this.available = available;
        this.requested = requested;
    }

    public String productName() {
        return productName;
    }

    public int available() {
        return available;
    }

    public int requested() {
        return requested;
    }
}
