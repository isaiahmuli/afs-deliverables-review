package com.example.payments.api;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/** The provider's payment report (provider/PROVIDER-API.md). */
@Schema(name = "PaymentReport")
public record PaymentReportRequest(
        @Schema(example = "evt-0001", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "is required") @Size(max = 100, message = "must be at most 100 characters") String eventId,
        @Schema(example = "chk-INV-1001", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "is required") @Size(max = 100, message = "must be at most 100 characters") String checkoutId,
        @Schema(example = "paid", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "is required") String status,
        @Schema(example = "120000", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "is required") @Positive(message = "must be above zero") Long amountCents) {
}
