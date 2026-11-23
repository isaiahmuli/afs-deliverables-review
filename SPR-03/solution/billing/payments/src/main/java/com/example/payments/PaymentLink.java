package com.example.payments;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "payment_link")
public class PaymentLink {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "invoice_id", nullable = false, unique = true)
    private Long invoiceId;

    @Column(name = "checkout_id", nullable = false, unique = true, length = 100)
    private String checkoutId;

    @Column(nullable = false, length = 500)
    private String url;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected PaymentLink() {
    }

    public PaymentLink(Long invoiceId, String checkoutId, String url) {
        this.invoiceId = invoiceId;
        this.checkoutId = checkoutId;
        this.url = url;
        this.createdAt = Instant.now();
    }

    public Long getId() { return id; }
    public Long getInvoiceId() { return invoiceId; }
    public String getCheckoutId() { return checkoutId; }
    public String getUrl() { return url; }
    public Instant getCreatedAt() { return createdAt; }
}
