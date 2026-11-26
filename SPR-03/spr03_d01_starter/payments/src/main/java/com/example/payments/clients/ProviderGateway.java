package com.example.payments.clients;

import com.example.payments.clients.ProviderClient.Checkout;
import com.example.payments.clients.ProviderClient.CheckoutRequest;
import org.springframework.stereotype.Component;

@Component
public class ProviderGateway {

    private final ProviderClient client;

    public ProviderGateway(ProviderClient client) {
        this.client = client;
    }

    public Checkout createCheckout(String reference, long amountCents) {
        return Downstream.call("payment provider",
                () -> client.createCheckout(new CheckoutRequest(reference, amountCents, "USD")));
    }
}
