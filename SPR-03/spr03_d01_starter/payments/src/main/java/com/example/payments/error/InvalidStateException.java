package com.example.payments.error;

import org.springframework.http.HttpStatus;

public class InvalidStateException extends ApiException {
    public InvalidStateException(String detail) {
        super(HttpStatus.CONFLICT, "invalid-state", "Not allowed in the invoice's current status", detail);
    }
}
