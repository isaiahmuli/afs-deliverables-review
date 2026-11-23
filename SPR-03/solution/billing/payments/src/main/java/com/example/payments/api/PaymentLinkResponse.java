package com.example.payments.api;

import com.example.payments.PaymentLink;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

@Schema(name = "PaymentLink")
public record PaymentLinkResponse(
        @Schema(example = "1001", requiredMode = Schema.RequiredMode.REQUIRED) long invoiceId,
        @Schema(example = "INV-1001", requiredMode = Schema.RequiredMode.REQUIRED) String invoiceNumber,
        @Schema(example = "chk-INV-1001", requiredMode = Schema.RequiredMode.REQUIRED) String checkoutId,
        @Schema(example = "https://pay.example.com/chk-INV-1001", requiredMode = Schema.RequiredMode.REQUIRED) String url,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Instant createdAt) {

    public static PaymentLinkResponse from(PaymentLink link) {
        return new PaymentLinkResponse(link.getInvoiceId(), "INV-" + link.getInvoiceId(), link.getCheckoutId(),
                link.getUrl(), link.getCreatedAt());
    }
}
