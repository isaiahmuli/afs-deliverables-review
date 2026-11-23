package com.example.payments.clients;

import com.example.payments.clients.InvoicesClient.InvoiceView;
import com.example.payments.clients.InvoicesClient.StatusChange;
import java.util.Optional;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;

/** Calls the invoices service; 404 and 409 are answers payments acts on, anything else is a failure. */
@Component
public class InvoicesGateway {

    /** The invoices service refused a status change (it answered 409). */
    public static class StatusChangeRefused extends RuntimeException {
        StatusChangeRefused() {
            super("invoices refused the status change");
        }
    }

    private final InvoicesClient client;

    public InvoicesGateway(InvoicesClient client) {
        this.client = client;
    }

    public Optional<InvoiceView> find(long invoiceId) {
        return Downstream.call("invoices service", () -> {
            try {
                return Optional.of(client.get(invoiceId));
            } catch (HttpClientErrorException.NotFound e) {
                return Optional.empty();
            }
        });
    }

    public InvoiceView markPaid(long invoiceId) {
        return Downstream.call("invoices service", () -> {
            try {
                return client.changeStatus(invoiceId, new StatusChange(InvoiceStatus.PAID));
            } catch (HttpClientErrorException.Conflict e) {
                throw new StatusChangeRefused();
            }
        });
    }
}
