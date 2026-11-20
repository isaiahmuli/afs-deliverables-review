package com.example.stock.service;

import java.time.Instant;
import java.util.List;

/** Read models returned by the services, so callers never touch lazy entities. */
public final class Views {

    private Views() {
    }

    public record ProductView(long id, String name, int quantity) {
    }

    public record AdjustmentView(long id, long productId, int quantityChange, int quantityAfter,
            String reason, Instant adjustedAt) {
    }

    public record StockLine(String product, int quantity, List<String> categories) {
    }

    public record SlipLine(long productId, int change, String reason) {
    }
}
