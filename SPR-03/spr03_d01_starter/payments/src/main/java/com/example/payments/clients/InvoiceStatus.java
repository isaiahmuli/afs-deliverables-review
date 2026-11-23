package com.example.payments.clients;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.util.Arrays;

/**
 * Invoice statuses as the invoices API reports them. This is payments' own view of that API, not
 * the invoices service's class: services don't share code. A status payments doesn't know yet
 * (for example one added later by invoices) becomes UNKNOWN instead of failing the call.
 */
public enum InvoiceStatus {
    DRAFT, SENT, PAID, CANCELLED, UNKNOWN;

    @JsonCreator
    public static InvoiceStatus from(String value) {
        return Arrays.stream(values()).filter(s -> s.name().equals(value)).findFirst().orElse(UNKNOWN);
    }
}
