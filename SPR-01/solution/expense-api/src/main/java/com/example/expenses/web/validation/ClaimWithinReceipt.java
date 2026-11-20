package com.example.expenses.web.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Domain rule: the claimed amount may be part or all of the receipt total, never more.
 * Placed on the submission type; the failure is reported on the {@code claimAmount} field.
 */
@Documented
@Constraint(validatedBy = ClaimWithinReceiptValidator.class)
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface ClaimWithinReceipt {

    String message() default "must not be more than the receipt total";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
