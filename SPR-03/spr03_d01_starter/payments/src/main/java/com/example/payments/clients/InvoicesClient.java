package com.example.payments.clients;

import java.math.BigDecimal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PutExchange;

/** The invoices service's API, as payments uses it. Base URL and timeouts: spring.http.serviceclient.invoices. */
@HttpExchange("/api/v1/invoices")
public interface InvoicesClient {

    @GetExchange("/{id}")
    InvoiceView get(@PathVariable long id);

    @PutExchange("/{id}/status")
    InvoiceView changeStatus(@PathVariable long id, @RequestBody StatusChange change);

    record InvoiceView(long id, String number, BigDecimal total, String currency, InvoiceStatus status) {
    }

    record StatusChange(InvoiceStatus status) {
    }
}
