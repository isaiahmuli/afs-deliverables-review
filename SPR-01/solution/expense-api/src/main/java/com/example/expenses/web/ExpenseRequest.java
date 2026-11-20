package com.example.expenses.web;

import com.example.expenses.domain.NewExpense;
import com.example.expenses.web.validation.ClaimAndReceipt;
import com.example.expenses.web.validation.ClaimWithinReceipt;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;

/** The JSON body of a submission. Amounts are US dollars. */
@ClaimWithinReceipt
public record ExpenseRequest(
        @NotBlank @Size(max = 100) String employee,
        @NotBlank @Size(max = 500) String description,
        @NotNull @PastOrPresent LocalDate expenseDate,
        @NotNull @DecimalMin(value = "0", inclusive = false, message = "must be greater than 0")
        @Digits(integer = 7, fraction = 2) BigDecimal receiptTotal,
        @NotNull @DecimalMin(value = "0", inclusive = false, message = "must be greater than 0")
        @Digits(integer = 7, fraction = 2) BigDecimal claimAmount) implements ClaimAndReceipt {

    NewExpense toNewExpense() {
        return new NewExpense(employee.strip(), description.strip(), expenseDate, receiptTotal, claimAmount);
    }
}
