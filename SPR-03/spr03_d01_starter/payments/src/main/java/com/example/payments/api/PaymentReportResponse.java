package com.example.payments.api;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "PaymentReportResult")
public record PaymentReportResponse(
        @Schema(example = "evt-0001", requiredMode = Schema.RequiredMode.REQUIRED) String eventId,
        @Schema(example = "1001", requiredMode = Schema.RequiredMode.REQUIRED) long invoiceId,
        @Schema(example = "INV-1001", requiredMode = Schema.RequiredMode.REQUIRED) String invoiceNumber,
        @Schema(allowableValues = "APPLIED", requiredMode = Schema.RequiredMode.REQUIRED) String outcome) {
}
