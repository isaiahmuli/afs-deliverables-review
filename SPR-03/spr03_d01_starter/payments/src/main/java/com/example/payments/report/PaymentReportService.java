package com.example.payments.report;

import com.example.payments.PaymentLink;
import com.example.payments.PaymentLinkRepository;
import com.example.payments.api.PaymentReportRequest;
import com.example.payments.api.PaymentReportResponse;
import com.example.payments.clients.InvoicesClient.InvoiceView;
import com.example.payments.clients.InvoicesGateway;
import com.example.payments.clients.InvoicesGateway.StatusChangeRefused;
import com.example.payments.error.CannotApplyException;
import com.example.payments.error.InvalidStateException;
import com.example.payments.error.ReportReusedException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Optional;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import tools.jackson.databind.json.JsonMapper;

/**
 * Applies the provider's payment report: the invoice becomes PAID through the invoices API.
 * The provider resends a report until it gets a success answer, so an applied report is stored by
 * eventId and a resend gets the stored answer without calling anything. Only applied reports are
 * stored: a report refused because invoices was down is applied when the provider resends it.
 */
@Service
public class PaymentReportService {

    public record Result(PaymentReportResponse response, boolean replayed) {
    }

    private final PaymentReportRepository reports;
    private final PaymentLinkRepository links;
    private final InvoicesGateway invoices;
    private final JsonMapper json;

    public PaymentReportService(PaymentReportRepository reports, PaymentLinkRepository links,
            InvoicesGateway invoices, JsonMapper json) {
        this.reports = reports;
        this.links = links;
        this.invoices = invoices;
        this.json = json;
    }

    public Result apply(PaymentReportRequest report) {
        String hash = fingerprint(report);
        Optional<Result> earlier = stored(report.eventId(), hash);
        if (earlier.isPresent()) {
            return earlier.get();
        }
        if (!"paid".equals(report.status())) {
            throw new CannotApplyException("Only paid reports are applied; this report's status is '%s'."
                    .formatted(report.status()));
        }
        PaymentLink link = links.findByCheckoutId(report.checkoutId()).orElseThrow(() -> new CannotApplyException(
                "Checkout %s is not one of ours.".formatted(report.checkoutId())));
        long invoiceId = link.getInvoiceId();
        InvoiceView invoice = invoices.find(invoiceId).orElseThrow(() -> new CannotApplyException(
                "Invoice INV-%d no longer exists.".formatted(invoiceId)));
        long expectedCents = invoice.total().movePointRight(2).longValueExact();
        if (report.amountCents() != expectedCents) {
            throw new CannotApplyException("Paid %s USD but invoice %s is %s USD; only full payments are applied."
                    .formatted(usd(report.amountCents()), invoice.number(), usd(expectedCents)));
        }
        try {
            invoices.markPaid(invoiceId);
        } catch (StatusChangeRefused e) {
            throw new InvalidStateException("Invoice %s is %s; the payment cannot be applied to it."
                    .formatted(invoice.number(), invoice.status()));
        }
        PaymentReportResponse response = new PaymentReportResponse(report.eventId(), invoiceId, invoice.number(), "APPLIED");
        try {
            reports.saveAndFlush(new PaymentReport(report.eventId(), hash, report.checkoutId(), invoiceId,
                    json.writeValueAsString(response)));
        } catch (DataIntegrityViolationException sameReportAtTheSameTime) {
            return stored(report.eventId(), hash).orElseThrow(() -> sameReportAtTheSameTime);
        }
        return new Result(response, false);
    }

    private Optional<Result> stored(String eventId, String hash) {
        return reports.findById(eventId).map(stored -> {
            if (!stored.getRequestHash().equals(hash)) {
                throw new ReportReusedException(eventId);
            }
            return new Result(json.readValue(stored.getResponseBody(), PaymentReportResponse.class), true);
        });
    }

    private static String usd(long cents) {
        return BigDecimal.valueOf(cents, 2).toPlainString();
    }

    static String fingerprint(PaymentReportRequest r) {
        String canonical = String.join("\u0000", r.eventId(), r.checkoutId(), r.status(), String.valueOf(r.amountCents()));
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(canonical.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
