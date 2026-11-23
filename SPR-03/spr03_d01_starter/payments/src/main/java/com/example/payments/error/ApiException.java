package com.example.payments.error;

import org.springframework.http.HttpStatus;

/** A refusal with a clear reason, turned into problem+json by {@link ApiExceptionHandler}. */
public abstract class ApiException extends RuntimeException {

    private final HttpStatus status;
    private final String typeName;
    private final String title;

    protected ApiException(HttpStatus status, String typeName, String title, String detail) {
        super(detail);
        this.status = status;
        this.typeName = typeName;
        this.title = title;
    }

    public HttpStatus status() { return status; }
    public String typeName() { return typeName; }
    public String title() { return title; }
}
