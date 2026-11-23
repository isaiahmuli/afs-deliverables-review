package com.example.payments;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalToJson;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.endsWith;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

/** Payment links, and D1-6: invoices answers, fails, or is too slow; the provider fails. */
class PaymentLinkTest extends PaymentsTestSupport {

    @Test
    void aSentInvoiceGetsALinkAndAskingAgainReturnsTheSameOne() throws Exception {
        long id = newInvoiceId();
        stubInvoice(id, "SENT", "1200.00");
        stubCheckoutCreated();

        requestLink(id).andExpect(status().isCreated())
                .andExpect(header().string("Location", endsWith("/api/v1/payment-links/" + id)))
                .andExpect(jsonPath("$.invoiceNumber").value("INV-" + id))
                .andExpect(jsonPath("$.checkoutId").value("chk-INV-" + id))
                .andExpect(jsonPath("$.url").value("https://pay.example.com/chk-INV-" + id));
        requestLink(id).andExpect(status().isOk())
                .andExpect(jsonPath("$.url").value("https://pay.example.com/chk-INV-" + id));
        mvc.perform(MockMvcRequestBuilders.get("/api/v1/payment-links/{id}", id)).andExpect(status().isOk())
                .andExpect(jsonPath("$.checkoutId").value("chk-INV-" + id));

        // The provider was asked once, with the reference and the amount in cents.
        downstream.verify(1, postRequestedFor(urlEqualTo("/checkouts")).withRequestBody(equalToJson(
                "{\"reference\":\"INV-" + id + "\",\"amountCents\":120000,\"currency\":\"USD\"}")));
    }

    @Test
    void draftCancelledAndPaidInvoicesAreRefused() throws Exception {
        for (String state : new String[] {"DRAFT", "CANCELLED", "PAID"}) {
            long id = newInvoiceId();
            stubInvoice(id, state, "340.00");
            requestLink(id).andExpect(status().isConflict())
                    .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                    .andExpect(jsonPath("$.type").value("https://billing.example.com/problems/invalid-state"))
                    .andExpect(jsonPath("$.detail").value("Invoice INV-%d is %s; a payment link needs a sent invoice."
                            .formatted(id, state)));
        }
        downstream.verify(0, postRequestedFor(urlEqualTo("/checkouts")));
    }

    /** A status invoices might add later is refused cleanly instead of breaking the call. */
    @Test
    void anInvoiceStatusPaymentsDoesNotKnowIsRefused() throws Exception {
        long id = newInvoiceId();
        stubInvoice(id, "ARCHIVED", "340.00");
        requestLink(id).andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("Invoice INV-%d is UNKNOWN; a payment link needs a sent invoice."
                        .formatted(id)));
        downstream.verify(0, postRequestedFor(urlEqualTo("/checkouts")));
    }

    @Test
    void anUnknownInvoiceIs404() throws Exception {
        long id = newInvoiceId();
        downstream.stubFor(get(urlEqualTo("/api/v1/invoices/" + id)).willReturn(aResponse()
                .withStatus(404).withHeader("Content-Type", "application/problem+json")
                .withBody("{\"type\":\"https://billing.example.com/problems/not-found\",\"title\":\"Not found\",\"status\":404,\"detail\":\"x\"}")));
        requestLink(id).andExpect(status().isNotFound())
                .andExpect(jsonPath("$.type").value("https://billing.example.com/problems/not-found"))
                .andExpect(jsonPath("$.detail").value("Invoice INV-%d does not exist.".formatted(id)));
        mvc.perform(MockMvcRequestBuilders.get("/api/v1/payment-links/{id}", id)).andExpect(status().isNotFound());
    }

    @Test
    void whenInvoicesFailsPaymentsReturnsItsOwn502() throws Exception {
        long id = newInvoiceId();
        downstream.stubFor(get(urlEqualTo("/api/v1/invoices/" + id)).willReturn(aResponse()
                .withStatus(500).withHeader("Content-Type", "text/plain")
                .withBody("java.lang.NullPointerException at InvoiceService.java:42")));
        requestLink(id).andExpect(status().isBadGateway())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value("https://billing.example.com/problems/downstream-failed"))
                .andExpect(jsonPath("$.title").value("Dependency failed"))
                .andExpect(jsonPath("$.status").value(502))
                .andExpect(jsonPath("$.detail").value("The invoices service failed to answer the request. Try again later."))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("NullPointer"))));
        assertThat(links.findByInvoiceId(id)).isEmpty();
    }

    @Test
    void whenInvoicesIsSlowerThanThreeSecondsPaymentsReturns504InsteadOfWaiting() throws Exception {
        long id = newInvoiceId();
        downstream.stubFor(get(urlEqualTo("/api/v1/invoices/" + id)).willReturn(aResponse()
                .withStatus(200).withHeader("Content-Type", "application/json")
                .withBody(invoiceJson(id, "SENT", "1200.00")).withFixedDelay(6_000)));

        long start = System.nanoTime();
        requestLink(id).andExpect(status().isGatewayTimeout())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value("https://billing.example.com/problems/downstream-timeout"))
                .andExpect(jsonPath("$.detail").value("The invoices service did not answer in time. Try again later."));
        double seconds = (System.nanoTime() - start) / 1e9;

        System.out.printf("[timeout] invoices delayed 6 s: payments answered 504 after %.2f s%n", seconds);
        assertThat(seconds).isBetween(2.9, 4.0);
        downstream.verify(0, postRequestedFor(urlEqualTo("/checkouts")));
    }

    @Test
    void whenTheProviderFailsOrIsSlowNoLinkIsStored() throws Exception {
        long id = newInvoiceId();
        stubInvoice(id, "SENT", "1200.00");
        downstream.stubFor(post(urlEqualTo("/checkouts")).willReturn(aResponse().withStatus(503)));
        requestLink(id).andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.detail").value("The payment provider failed to answer the request. Try again later."));

        downstream.stubFor(post(urlEqualTo("/checkouts")).willReturn(aResponse().withStatus(201)
                .withHeader("Content-Type", "application/json")
                .withBody("{\"checkoutId\":\"chk-INV-" + id + "\",\"url\":\"https://pay.example.com/chk-INV-" + id + "\"}")
                .withFixedDelay(6_000)));
        requestLink(id).andExpect(status().isGatewayTimeout())
                .andExpect(jsonPath("$.detail").value("The payment provider did not answer in time. Try again later."));
        assertThat(links.findByInvoiceId(id)).isEmpty();

        stubCheckoutCreated();
        requestLink(id).andExpect(status().isCreated());
    }

    @Test
    void anInvalidRequestIsRefused() throws Exception {
        mvc.perform(MockMvcRequestBuilders.post("/api/v1/payment-links")
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value("https://billing.example.com/problems/invalid-request"))
                .andExpect(jsonPath("$.detail").value("Fix these fields: invoiceId is required."));
    }
}
