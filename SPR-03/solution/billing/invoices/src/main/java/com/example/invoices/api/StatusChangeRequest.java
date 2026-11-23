package com.example.invoices.api;

import com.example.invoices.InvoiceStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(name = "StatusChange")
public record StatusChangeRequest(
        @Schema(example = "SENT", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "is required")
        InvoiceStatus status) {
}
