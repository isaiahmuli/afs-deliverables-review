package com.example.invoices.api;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

@Schema(name = "NewInvoice")
public record CreateInvoiceRequest(
        @Schema(example = "Harbor Bakery", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "is required") @Size(max = 200, message = "must be at most 200 characters")
        String customer,

        @Schema(example = "40 hours of bookkeeping, October", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "is required") @Size(max = 2000, message = "must be at most 2000 characters")
        String description,

        @Schema(description = "USD, above zero, at most two decimals", example = "1200.00",
                requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "is required")
        @DecimalMin(value = "0.00", inclusive = false, message = "must be above zero")
        @Digits(integer = 10, fraction = 2, message = "must have at most two decimal places")
        BigDecimal total) {
}
