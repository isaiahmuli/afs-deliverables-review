package com.example.stock.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@Table(name = "product")
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(nullable = false)
    private int quantity;

    /** Optimistic lock: an update based on an older version is rejected. */
    @Version
    @Column(nullable = false)
    private long version;

    /** Owning side of the many-to-many, stored in the product_category join table. Lazy. */
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "product_category",
            joinColumns = @JoinColumn(name = "product_id"),
            inverseJoinColumns = @JoinColumn(name = "category_id"))
    private Set<Category> categories = new HashSet<>();

    /** One-to-many history. Read-only from this side; adjustments are saved on their own. */
    @OneToMany(mappedBy = "product", fetch = FetchType.LAZY)
    @OrderBy("adjustedAt ASC, id ASC")
    private List<StockAdjustment> adjustments = new ArrayList<>();

    protected Product() {
    }

    public Product(String name) {
        this.name = Names.require(name, "product name");
    }

    /** Adds stock. The returned adjustment must be saved in the same transaction. */
    public StockAdjustment receive(int units, String reason, Instant at) {
        requirePositive(units);
        return change(Math.addExact(quantity, units), units, reason, at);
    }

    /** Removes stock, never below zero. The returned adjustment must be saved in the same transaction. */
    public StockAdjustment use(int units, String reason, Instant at) throws InsufficientStockException {
        requirePositive(units);
        if (units > quantity) {
            throw new InsufficientStockException(name, quantity, units);
        }
        return change(quantity - units, -units, reason, at);
    }

    /** A signed, non-zero change: positive receives, negative uses. */
    public StockAdjustment apply(int change, String reason, Instant at) throws InsufficientStockException {
        if (change == 0) {
            throw new IllegalArgumentException("an adjustment must add or remove a non-zero quantity");
        }
        return change > 0 ? receive(change, reason, at) : use(Math.negateExact(change), reason, at);
    }

    private StockAdjustment change(int after, int change, String reason, Instant at) {
        String why = Names.require(reason, "reason");
        quantity = after;
        return new StockAdjustment(this, change, after, why, at);
    }

    private static void requirePositive(int units) {
        if (units <= 0) {
            throw new IllegalArgumentException("quantity must be greater than 0, was " + units);
        }
    }

    public void replaceCategories(Collection<Category> newCategories) {
        categories.clear();
        categories.addAll(newCategories);
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public int getQuantity() {
        return quantity;
    }

    public long getVersion() {
        return version;
    }

    public Set<Category> getCategories() {
        return Set.copyOf(categories);
    }

    public List<StockAdjustment> getAdjustments() {
        return List.copyOf(adjustments);
    }
}
