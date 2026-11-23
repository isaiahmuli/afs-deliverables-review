package com.example.payments;

import com.example.payments.clients.InvoiceStatus;
import com.example.payments.clients.InvoicesClient.InvoiceView;
import com.example.payments.clients.InvoicesGateway;
import com.example.payments.clients.ProviderClient.Checkout;
import com.example.payments.clients.ProviderGateway;
import com.example.payments.error.InvalidStateException;
import com.example.payments.error.NotFoundException;
import java.math.BigDecimal;
import java.util.Optional;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

/**
 * A payment link is only for a sent invoice. The first request creates a checkout with the
 * provider and stores it; later requests for the same invoice get the same link.
 * No database transaction is held open while other services are called.
 */
@Service
public class PaymentLinkService {

    public record Result(PaymentLink link, boolean created) {
    }

    private final PaymentLinkRepository links;
    private final InvoicesGateway invoices;
    private final ProviderGateway provider;

    public PaymentLinkService(PaymentLinkRepository links, InvoicesGateway invoices, ProviderGateway provider) {
        this.links = links;
        this.invoices = invoices;
        this.provider = provider;
    }

    public Result linkFor(long invoiceId) {
        InvoiceView invoice = invoices.find(invoiceId).orElseThrow(() -> new NotFoundException(
                "Invoice INV-%d does not exist.".formatted(invoiceId)));
        if (invoice.status() != InvoiceStatus.SENT) {
            throw new InvalidStateException("Invoice %s is %s; a payment link needs a sent invoice."
                    .formatted(invoice.number(), invoice.status()));
        }
        Optional<PaymentLink> existing = links.findByInvoiceId(invoiceId);
        if (existing.isPresent()) {
            return new Result(existing.get(), false);
        }
        Checkout checkout = provider.createCheckout(invoice.number(), cents(invoice.total()));
        try {
            return new Result(links.saveAndFlush(new PaymentLink(invoiceId, checkout.checkoutId(), checkout.url())), true);
        } catch (DataIntegrityViolationException sameInvoiceAtTheSameTime) {
            return new Result(links.findByInvoiceId(invoiceId).orElseThrow(() -> sameInvoiceAtTheSameTime), false);
        }
    }

    public PaymentLink get(long invoiceId) {
        return links.findByInvoiceId(invoiceId).orElseThrow(() -> new NotFoundException(
                "Invoice INV-%d has no payment link.".formatted(invoiceId)));
    }

    static long cents(BigDecimal usd) {
        return usd.movePointRight(2).longValueExact();
    }
}
