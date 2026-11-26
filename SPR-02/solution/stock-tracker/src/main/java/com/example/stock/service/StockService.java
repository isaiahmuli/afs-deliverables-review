package com.example.stock.service;

import com.example.stock.domain.InsufficientStockException;
import com.example.stock.domain.StockAdjustment;
import com.example.stock.repository.ProductRepository;
import com.example.stock.repository.StockAdjustmentRepository;
import com.example.stock.service.Views.AdjustmentView;
import com.example.stock.service.Views.SlipLine;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Receiving and using supplies. Every failure leaves stock and history unchanged. */
@Service
public class StockService {

    private final StockLedger ledger;
    private final ProductRepository products;
    private final StockAdjustmentRepository adjustments;

    public StockService(StockLedger ledger, ProductRepository products, StockAdjustmentRepository adjustments) {
        this.ledger = ledger;
        this.products = products;
        this.adjustments = adjustments;
    }

    @Transactional
    public AdjustmentView receive(long productId, int units, String reason) {
        requirePositive(units);
        try {
            return view(ledger.apply(productId, units, reason));
        } catch (InsufficientStockException e) {
            throw new IllegalStateException("receiving stock cannot make it negative", e);
        }
    }

    // rollbackFor: InsufficientStockException is checked, and Spring would otherwise commit.
    @Transactional(rollbackFor = InsufficientStockException.class)
    public AdjustmentView use(long productId, int units, String reason) throws InsufficientStockException {
        requirePositive(units);
        return view(ledger.apply(productId, -units, reason));
    }

    /** Several changes from one delivery note or usage slip: all are applied, or none. */
    @Transactional(rollbackFor = InsufficientStockException.class)
    public List<AdjustmentView> applySlip(List<SlipLine> lines) throws InsufficientStockException {
        List<AdjustmentView> applied = new ArrayList<>();
        for (SlipLine line : lines) {
            applied.add(view(ledger.apply(line.productId(), line.change(), line.reason())));
        }
        return applied;
    }

    @Transactional(readOnly = true)
    public List<AdjustmentView> history(long productId) {
        if (!products.existsById(productId)) {
            throw new ProductNotFoundException(productId);
        }
        return adjustments.findByProductIdOrderByAdjustedAtAscIdAsc(productId).stream().map(StockService::view).toList();
    }

    @Transactional(readOnly = true)
    public int quantity(long productId) {
        return products.findById(productId).orElseThrow(() -> new ProductNotFoundException(productId)).getQuantity();
    }

    private static void requirePositive(int units) {
        if (units <= 0) {
            throw new IllegalArgumentException("quantity must be greater than 0, was " + units);
        }
    }

    static AdjustmentView view(StockAdjustment a) {
        return new AdjustmentView(a.getId(), a.getProduct().getId(), a.getQuantityChange(), a.getQuantityAfter(),
                a.getReason(), a.getAdjustedAt());
    }
}
