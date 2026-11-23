package com.example.invoices.idempotency;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import java.time.Instant;
import org.springframework.data.domain.Persistable;

@Entity
@Table(name = "idempotency_record")
public class IdempotencyRecord implements Persistable<String> {

    @Id
    @Column(name = "idempotency_key", length = 255)
    private String key;

    @Column(name = "request_hash", nullable = false, length = 64)
    private String requestHash;

    @Column(name = "response_status", nullable = false)
    private int responseStatus;

    @Column(name = "response_body", nullable = false, columnDefinition = "text")
    private String responseBody;

    @Column(nullable = false, length = 500)
    private String location;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Transient
    private boolean isNew;

    protected IdempotencyRecord() {
    }

    public IdempotencyRecord(String key, String requestHash, int responseStatus, String responseBody, String location) {
        this.key = key;
        this.requestHash = requestHash;
        this.responseStatus = responseStatus;
        this.responseBody = responseBody;
        this.location = location;
        this.createdAt = Instant.now();
        this.isNew = true;
    }

    @Override public String getId() { return key; }
    /** Always an INSERT, so two requests racing with one key collide on the primary key. */
    @Override public boolean isNew() { return isNew; }

    public String getRequestHash() { return requestHash; }
    public int getResponseStatus() { return responseStatus; }
    public String getResponseBody() { return responseBody; }
    public String getLocation() { return location; }
}
