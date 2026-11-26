package com.example.stock.service;

/** Someone else changed this product first. Nothing was written; reload and try again. */
public class ConcurrentStockChangeException extends RuntimeException {

    public ConcurrentStockChangeException(long productId, Throwable cause) {
        super("Product " + productId + " was changed by someone else at the same time. "
                + "Nothing was saved; try again with the new count.", cause);
    }
}
