package com.example.payments.error;

import org.springframework.http.HttpStatus;

public class ReportReusedException extends ApiException {
    public ReportReusedException(String eventId) {
        super(HttpStatus.UNPROCESSABLE_CONTENT, "idempotency-key-reused", "Event id already used",
                "Payment report %s was already received with different content.".formatted(eventId));
    }
}
