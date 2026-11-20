package com.example.expenses.domain;

import java.math.BigDecimal;
import java.time.LocalDate;

/** A validated submission that has not been saved yet. */
public record NewExpense(
        String employee,
        String description,
        LocalDate expenseDate,
        BigDecimal receiptTotal,
        BigDecimal claimAmount) {
}
