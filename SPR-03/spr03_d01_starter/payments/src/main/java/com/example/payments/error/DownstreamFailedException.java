package com.example.payments.error;

import org.springframework.http.HttpStatus;

public class DownstreamFailedException extends ApiException {
    public DownstreamFailedException(String service) {
        super(HttpStatus.BAD_GATEWAY, "downstream-failed", "Dependency failed", "The %s failed to answer the request. Try again later.".formatted(service));
    }
}
