package com.example.invoices;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "invoice")
public class Invoice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String customer;

    @Column(nullable = false, length = 2000)
    private String description;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal total;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private InvoiceStatus status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected Invoice() {
    }

    public Invoice(String customer, String description, BigDecimal total) {
        this.customer = customer;
        this.description = description;
        this.total = total;
        this.status = InvoiceStatus.DRAFT;
        this.createdAt = Instant.now();
    }

    public Long getId() { return id; }
    public String getCustomer() { return customer; }
    public String getDescription() { return description; }
    public BigDecimal getTotal() { return total; }
    public InvoiceStatus getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }

    public void setStatus(InvoiceStatus status) { this.status = status; }
}
