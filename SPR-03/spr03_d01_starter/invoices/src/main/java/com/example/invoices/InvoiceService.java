package com.example.invoices;

import com.example.invoices.api.CreateInvoiceRequest;
import com.example.invoices.error.InvalidStateException;
import com.example.invoices.error.NotFoundException;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InvoiceService {

    private final InvoiceRepository invoices;

    public InvoiceService(InvoiceRepository invoices) {
        this.invoices = invoices;
    }

    @Transactional
    public Invoice create(CreateInvoiceRequest request) {
        return invoices.saveAndFlush(
                new Invoice(request.customer().strip(), request.description().strip(), request.total()));
    }

    @Transactional(readOnly = true)
    public Invoice get(long id) {
        return invoices.findById(id).orElseThrow(() -> notFound(id));
    }

    @Transactional(readOnly = true)
    public List<Invoice> list() {
        return invoices.findAllByOrderByIdAsc();
    }

    /** Setting the status the invoice already has succeeds and changes nothing. */
    @Transactional
    public Invoice changeStatus(long id, InvoiceStatus target) {
        Invoice invoice = invoices.findForUpdateById(id).orElseThrow(() -> notFound(id));
        InvoiceStatus current = invoice.getStatus();
        if (current == target) {
            return invoice;
        }
        if (!current.next().contains(target)) {
            throw new InvalidStateException("Invoice %s is %s and cannot become %s."
                    .formatted(number(id), current, target));
        }
        invoice.setStatus(target);
        return invoice;
    }

    /** Only drafts can be deleted. A second delete finds nothing: 404, and nothing changes. */
    @Transactional
    public void delete(long id) {
        Invoice invoice = invoices.findForUpdateById(id).orElseThrow(() -> notFound(id));
        if (invoice.getStatus() != InvoiceStatus.DRAFT) {
            throw new InvalidStateException("Invoice %s is %s; only draft invoices can be deleted."
                    .formatted(number(id), invoice.getStatus()));
        }
        invoices.delete(invoice);
    }

    public static String number(long id) {
        return "INV-" + id;
    }

    private static NotFoundException notFound(long id) {
        return new NotFoundException("Invoice %s does not exist.".formatted(number(id)));
    }
}
