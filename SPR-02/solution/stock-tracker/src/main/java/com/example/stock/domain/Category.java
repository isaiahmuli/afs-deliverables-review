package com.example.stock.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "category")
public class Category {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    /** Inverse side of the many-to-many; the join table is owned by {@link Product}. */
    @ManyToMany(mappedBy = "categories")
    private Set<Product> products = new HashSet<>();

    protected Category() {
    }

    public Category(String name) {
        this.name = Names.require(name, "category name");
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public Set<Product> getProducts() {
        return Set.copyOf(products);
    }
}
