package com.example.payments.report;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import java.time.Instant;
import org.springframework.data.domain.Persistable;

/** A payment report that was applied, kept so the same report arriving again changes nothing. */
@Entity
@Table(name = "payment_report")
public class PaymentReport implements Persistable<String> {

    @Id
    @Column(name = "event_id", length = 100)
    private String eventId;

    @Column(name = "request_hash", nullable = false, length = 64)
    private String requestHash;

    @Column(name = "checkout_id", nullable = false, length = 100)
    private String checkoutId;

    @Column(name = "invoice_id", nullable = false)
    private Long invoiceId;

    @Column(name = "response_body", nullable = false, columnDefinition = "text")
    private String responseBody;

    @Column(name = "received_at", nullable = false, updatable = false)
    private Instant receivedAt;

    @Transient
    private boolean isNew;

    protected PaymentReport() {
    }

    public PaymentReport(String eventId, String requestHash, String checkoutId, Long invoiceId, String responseBody) {
        this.eventId = eventId;
        this.requestHash = requestHash;
        this.checkoutId = checkoutId;
        this.invoiceId = invoiceId;
        this.responseBody = responseBody;
        this.receivedAt = Instant.now();
        this.isNew = true;
    }

    @Override public String getId() { return eventId; }
    @Override public boolean isNew() { return isNew; }

    public String getRequestHash() { return requestHash; }
    public String getResponseBody() { return responseBody; }
    public Long getInvoiceId() { return invoiceId; }
}
