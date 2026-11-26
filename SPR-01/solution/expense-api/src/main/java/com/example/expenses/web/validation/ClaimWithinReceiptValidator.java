package com.example.expenses.web.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class ClaimWithinReceiptValidator implements ConstraintValidator<ClaimWithinReceipt, ClaimAndReceipt> {

    @Override
    public boolean isValid(ClaimAndReceipt value, ConstraintValidatorContext context) {
        if (value == null || value.claimAmount() == null || value.receiptTotal() == null) {
            return true; // missing values are reported by @NotNull on the fields
        }
        if (value.claimAmount().compareTo(value.receiptTotal()) <= 0) {
            return true;
        }
        context.disableDefaultConstraintViolation();
        context.buildConstraintViolationWithTemplate(context.getDefaultConstraintMessageTemplate())
                .addPropertyNode("claimAmount")
                .addConstraintViolation();
        return false;
    }
}
