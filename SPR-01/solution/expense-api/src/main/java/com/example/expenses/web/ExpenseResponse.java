package com.example.expenses.web;

import com.example.expenses.domain.Expense;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public record ExpenseResponse(
        long id,
        String employee,
        String description,
        LocalDate expenseDate,
        BigDecimal receiptTotal,
        BigDecimal claimAmount,
        String currency,
        Instant submittedAt) {

    static ExpenseResponse from(Expense e) {
        return new ExpenseResponse(e.id(), e.employee(), e.description(), e.expenseDate(),
                e.receiptTotal(), e.claimAmount(), "USD", e.submittedAt());
    }
}
