package com.example.payments.error;

import org.springframework.http.HttpStatus;

public class DownstreamTimeoutException extends ApiException {
    public DownstreamTimeoutException(String service) {
        super(HttpStatus.GATEWAY_TIMEOUT, "downstream-timeout", "Dependency timed out", "The %s did not answer in time. Try again later.".formatted(service));
    }
}
