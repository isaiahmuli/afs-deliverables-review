package com.example.stock.service;

import com.example.stock.domain.InsufficientStockException;
import com.example.stock.domain.Product;
import com.example.stock.domain.StockAdjustment;
import com.example.stock.repository.ProductRepository;
import com.example.stock.repository.StockAdjustmentRepository;
import java.time.Clock;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Applies one stock change: the new count and its history row, written together. Must run
 * inside the caller's transaction, which decides what is committed or rolled back.
 */
@Component
public class StockLedger {

    private final ProductRepository products;
    private final StockAdjustmentRepository adjustments;
    private final Clock clock;

    public StockLedger(ProductRepository products, StockAdjustmentRepository adjustments, Clock clock) {
        this.products = products;
        this.adjustments = adjustments;
        this.clock = clock;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public StockAdjustment apply(long productId, int change, String reason) throws InsufficientStockException {
        Product product = products.findById(productId).orElseThrow(() -> new ProductNotFoundException(productId));
        StockAdjustment adjustment = product.apply(change, reason, clock.instant());
        try {
            // Flush now so a version conflict is reported here, not later at commit.
            products.saveAndFlush(product);
        } catch (OptimisticLockingFailureException e) {
            throw new ConcurrentStockChangeException(productId, e);
        }
        return adjustments.save(adjustment);
    }
}
