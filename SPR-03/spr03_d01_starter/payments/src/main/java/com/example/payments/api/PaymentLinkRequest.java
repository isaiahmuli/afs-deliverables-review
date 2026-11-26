package com.example.payments.api;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

@Schema(name = "PaymentLinkRequest")
public record PaymentLinkRequest(
        @Schema(example = "1001", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "is required") @Positive(message = "must be a positive invoice id")
        Long invoiceId) {
}
