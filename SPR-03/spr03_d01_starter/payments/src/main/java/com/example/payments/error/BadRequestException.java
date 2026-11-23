package com.example.payments.error;

import org.springframework.http.HttpStatus;

public class BadRequestException extends ApiException {
    public BadRequestException(String detail) {
        super(HttpStatus.BAD_REQUEST, "invalid-request", "Invalid request", detail);
    }
}
