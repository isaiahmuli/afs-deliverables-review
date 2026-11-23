package com.example.payments;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.anyRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.anyUrl;
import static com.github.tomakehurst.wiremock.client.WireMock.put;
import static com.github.tomakehurst.wiremock.client.WireMock.putRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

/** Payment reports: applied once, replays change nothing, and refusals for what can't be applied. */
class PaymentReportTest extends PaymentsTestSupport {

    private long sentInvoiceWithLink(String total) throws Exception {
        long id = newInvoiceId();
        stubInvoice(id, "SENT", total);
        stubCheckoutCreated();
        requestLink(id).andExpect(status().isCreated());
        downstream.resetRequests();
        return id;
    }

    private static String eventId() {
        return "evt-" + UUID.randomUUID();
    }

    @Test
    void aFullPaymentMarksTheInvoicePaid() throws Exception {
        long id = sentInvoiceWithLink("1200.00");
        stubMarkPaid(id, "1200.00");
        String event = eventId();

        sendReport(event, "chk-INV-" + id, "paid", 120_000).andExpect(status().isOk())
                .andExpect(jsonPath("$.eventId").value(event))
                .andExpect(jsonPath("$.invoiceId").value(id))
                .andExpect(jsonPath("$.invoiceNumber").value("INV-" + id))
                .andExpect(jsonPath("$.outcome").value("APPLIED"));
        downstream.verify(1, putRequestedFor(urlEqualTo("/api/v1/invoices/" + id + "/status")));
    }

    @Test
    void theSameReportAgainGetsTheSameAnswerAndCallsNothing() throws Exception {
        long id = sentInvoiceWithLink("1200.00");
        stubMarkPaid(id, "1200.00");
        String event = eventId();
        String first = sendReport(event, "chk-INV-" + id, "paid", 120_000).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        downstream.resetRequests();

        sendReport(event, "chk-INV-" + id, "paid", 120_000).andExpect(status().isOk())
                .andExpect(header().string("Idempotent-Replayed", "true"))
                .andExpect(content().json(first));
        sendReport(event, "chk-INV-" + id, "paid", 120_000).andExpect(status().isOk());
        downstream.verify(0, anyRequestedFor(anyUrl()));
        System.out.printf("[report replay] %s sent 3 times: applied once, 0 calls to invoices for the 2 resends%n",
                event.substring(0, 12));
    }

    @Test
    void theSameEventIdWithDifferentContentIsRefused() throws Exception {
        long id = sentInvoiceWithLink("1200.00");
        stubMarkPaid(id, "1200.00");
        String event = eventId();
        sendReport(event, "chk-INV-" + id, "paid", 120_000).andExpect(status().isOk());
        sendReport(event, "chk-INV-" + id, "paid", 60_000).andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.type").value("https://billing.example.com/problems/idempotency-key-reused"))
                .andExpect(jsonPath("$.detail").value("Payment report %s was already received with different content.".formatted(event)));
    }

    @Test
    void aNewReportForAnAlreadyPaidInvoiceIsProcessedNormally() throws Exception {
        long id = sentInvoiceWithLink("1200.00");
        stubMarkPaid(id, "1200.00");
        sendReport(eventId(), "chk-INV-" + id, "paid", 120_000).andExpect(status().isOk());
        sendReport(eventId(), "chk-INV-" + id, "paid", 120_000).andExpect(status().isOk());
        downstream.verify(2, putRequestedFor(urlEqualTo("/api/v1/invoices/" + id + "/status")));
    }

    @Test
    void reportsThatCannotBeAppliedAre422AndChangeNothing() throws Exception {
        long id = sentInvoiceWithLink("1200.00");
        stubMarkPaid(id, "1200.00");

        sendReport(eventId(), "chk-INV-" + id, "paid", 100_000).andExpect(status().isUnprocessableContent())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value("https://billing.example.com/problems/cannot-apply"))
                .andExpect(jsonPath("$.title").value("Payment report cannot be applied"))
                .andExpect(jsonPath("$.detail").value(
                        "Paid 1000.00 USD but invoice INV-%d is 1200.00 USD; only full payments are applied.".formatted(id)));
        sendReport(eventId(), "chk-unknown", "paid", 120_000).andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.detail").value("Checkout chk-unknown is not one of ours."));
        sendReport(eventId(), "chk-INV-" + id, "failed", 120_000).andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.detail").value("Only paid reports are applied; this report's status is 'failed'."));

        downstream.verify(0, putRequestedFor(urlEqualTo("/api/v1/invoices/" + id + "/status")));
    }

    @Test
    void aPaymentForACancelledInvoiceIs409() throws Exception {
        long id = sentInvoiceWithLink("340.00");
        stubInvoice(id, "CANCELLED", "340.00");
        downstream.stubFor(put(urlEqualTo("/api/v1/invoices/" + id + "/status")).willReturn(aResponse()
                .withStatus(409).withHeader("Content-Type", "application/problem+json")
                .withBody("{\"type\":\"https://billing.example.com/problems/invalid-state\",\"title\":\"x\",\"status\":409,\"detail\":\"x\"}")));
        sendReport(eventId(), "chk-INV-" + id, "paid", 34_000).andExpect(status().isConflict())
                .andExpect(jsonPath("$.type").value("https://billing.example.com/problems/invalid-state"))
                .andExpect(jsonPath("$.detail").value(
                        "Invoice INV-%d is CANCELLED; the payment cannot be applied to it.".formatted(id)));
    }

    /** A report refused because invoices was slow is not stored, so the provider's resend is applied. */
    @Test
    void whenInvoicesTimesOutTheReportIsRefusedAndTheResendIsApplied() throws Exception {
        long id = sentInvoiceWithLink("1200.00");
        downstream.stubFor(put(urlEqualTo("/api/v1/invoices/" + id + "/status")).willReturn(aResponse()
                .withStatus(200).withFixedDelay(6_000)));
        String event = eventId();
        sendReport(event, "chk-INV-" + id, "paid", 120_000).andExpect(status().isGatewayTimeout())
                .andExpect(jsonPath("$.type").value("https://billing.example.com/problems/downstream-timeout"));

        stubMarkPaid(id, "1200.00");
        sendReport(event, "chk-INV-" + id, "paid", 120_000).andExpect(status().isOk())
                .andExpect(jsonPath("$.outcome").value("APPLIED"));
    }

    @Test
    void whenInvoicesFailsTheReportGets502() throws Exception {
        long id = sentInvoiceWithLink("1200.00");
        downstream.stubFor(put(urlEqualTo("/api/v1/invoices/" + id + "/status"))
                .willReturn(aResponse().withStatus(503)));
        sendReport(eventId(), "chk-INV-" + id, "paid", 120_000).andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.detail").value("The invoices service failed to answer the request. Try again later."));
    }
}
