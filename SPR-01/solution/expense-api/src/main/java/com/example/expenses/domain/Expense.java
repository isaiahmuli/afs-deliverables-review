package com.example.expenses.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/** A saved expense claim. Amounts are US dollars. */
public record Expense(
        long id,
        String employee,
        String description,
        LocalDate expenseDate,
        BigDecimal receiptTotal,
        BigDecimal claimAmount,
        Instant submittedAt) {
}
