package com.example.invoices;

import java.util.Set;

public enum InvoiceStatus {
    DRAFT, SENT, PAID, CANCELLED;

    /** The statuses this one may change to (not counting itself, which is always a no-op). */
    public Set<InvoiceStatus> next() {
        return switch (this) {
            case DRAFT -> Set.of(SENT, CANCELLED);
            case SENT -> Set.of(PAID, CANCELLED);
            case PAID, CANCELLED -> Set.of();
        };
    }
}
