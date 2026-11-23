package com.example.invoices.api;

import com.example.invoices.Invoice;
import com.example.invoices.InvoiceService;
import com.example.invoices.InvoiceStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.Instant;

@Schema(name = "Invoice")
public record InvoiceResponse(
        @Schema(example = "1001", requiredMode = Schema.RequiredMode.REQUIRED) long id,
        @Schema(example = "INV-1001", requiredMode = Schema.RequiredMode.REQUIRED) String number,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String customer,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String description,
        @Schema(example = "1200.00", requiredMode = Schema.RequiredMode.REQUIRED) BigDecimal total,
        @Schema(allowableValues = "USD", requiredMode = Schema.RequiredMode.REQUIRED) String currency,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) InvoiceStatus status,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Instant createdAt) {

    public static InvoiceResponse from(Invoice invoice) {
        return new InvoiceResponse(invoice.getId(), InvoiceService.number(invoice.getId()),
                invoice.getCustomer(), invoice.getDescription(), invoice.getTotal(), "USD",
                invoice.getStatus(), invoice.getCreatedAt());
    }
}
