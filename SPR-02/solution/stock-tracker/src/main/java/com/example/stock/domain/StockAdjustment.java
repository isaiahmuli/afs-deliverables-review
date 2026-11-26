package com.example.stock.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;

/** One change to a product's stock, with its reason. Never edited after it is written. */
@Entity
@Table(name = "stock_adjustment")
public class StockAdjustment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(name = "quantity_change", nullable = false)
    private int quantityChange;

    @Column(name = "quantity_after", nullable = false)
    private int quantityAfter;

    @Column(nullable = false, length = 500)
    private String reason;

    @Column(name = "adjusted_at", nullable = false)
    private Instant adjustedAt;

    protected StockAdjustment() {
    }

    StockAdjustment(Product product, int quantityChange, int quantityAfter, String reason, Instant adjustedAt) {
        this.product = product;
        this.quantityChange = quantityChange;
        this.quantityAfter = quantityAfter;
        this.reason = reason;
        this.adjustedAt = adjustedAt;
    }

    public Long getId() {
        return id;
    }

    public Product getProduct() {
        return product;
    }

    public int getQuantityChange() {
        return quantityChange;
    }

    public int getQuantityAfter() {
        return quantityAfter;
    }

    public String getReason() {
        return reason;
    }

    public Instant getAdjustedAt() {
        return adjustedAt;
    }
}
