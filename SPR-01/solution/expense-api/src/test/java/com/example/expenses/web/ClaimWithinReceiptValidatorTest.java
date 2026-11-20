package com.example.expenses.web;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class ClaimWithinReceiptValidatorTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    private Set<ConstraintViolation<ExpenseRequest>> validate(String receipt, String claim) {
        return validator.validate(new ExpenseRequest("Amina", "Fuel", LocalDate.parse("2026-11-18"),
                new BigDecimal(receipt), new BigDecimal(claim)));
    }

    @ParameterizedTest(name = "claim {1} of receipt {0} is allowed")
    @CsvSource({"120.00, 50.00", "120.00, 120.00", "120, 119.99", "0.01, 0.01"})
    void partOrAllOfTheReceiptIsAllowed(String receipt, String claim) {
        assertThat(validate(receipt, claim)).isEmpty();
    }

    @ParameterizedTest(name = "claim {1} of receipt {0} is refused")
    @CsvSource({"50.00, 80.00", "120.00, 120.01"})
    void moreThanTheReceiptIsRefusedOnClaimAmount(String receipt, String claim) {
        assertThat(validate(receipt, claim))
                .singleElement()
                .satisfies(v -> {
                    assertThat(v.getPropertyPath()).hasToString("claimAmount");
                    assertThat(v.getMessage()).isEqualTo("must not be more than the receipt total");
                });
    }

    @Test
    void missingAmountsAreLeftToNotNull() {
        var violations = validator.validate(new ExpenseRequest("Amina", "Fuel",
                LocalDate.parse("2026-11-18"), null, new BigDecimal("10.00")));

        assertThat(violations).extracting(v -> v.getPropertyPath().toString()).containsExactly("receiptTotal");
    }
}
