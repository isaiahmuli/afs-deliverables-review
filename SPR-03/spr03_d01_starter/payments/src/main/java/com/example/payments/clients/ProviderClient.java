package com.example.payments.clients;

import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.service.annotation.PostExchange;

/** The payment provider (provider/PROVIDER-API.md). Base URL and timeouts: spring.http.serviceclient.provider. */
public interface ProviderClient {

    @PostExchange("/checkouts")
    Checkout createCheckout(@RequestBody CheckoutRequest request);

    record CheckoutRequest(String reference, long amountCents, String currency) {
    }

    record Checkout(String checkoutId, String url) {
    }
}
