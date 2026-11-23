package com.example.invoices.error;

import org.springframework.http.HttpStatus;

public class IdempotencyKeyReusedException extends ApiException {
    public IdempotencyKeyReusedException() {
        super(HttpStatus.UNPROCESSABLE_CONTENT, "idempotency-key-reused", "Idempotency key already used",
                "This Idempotency-Key was already used for a different invoice. Use a new key for a new request.");
    }
}
