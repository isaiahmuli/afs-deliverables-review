package com.example.payments.error;

import org.springframework.http.HttpStatus;

public class CannotApplyException extends ApiException {
    public CannotApplyException(String detail) {
        super(HttpStatus.UNPROCESSABLE_CONTENT, "cannot-apply", "Payment report cannot be applied", detail);
    }
}
