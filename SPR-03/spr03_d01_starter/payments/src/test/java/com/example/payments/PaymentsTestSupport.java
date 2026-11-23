package com.example.payments;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalToJson;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.put;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;

import com.example.payments.report.PaymentReportRepository;
import com.github.tomakehurst.wiremock.WireMockServer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

/**
 * One WireMock server stands in for both the invoices service and the payment provider. The
 * payments database is the Compose one; each test uses fresh invoice ids and removes its rows.
 */
@SpringBootTest
@AutoConfigureMockMvc
abstract class PaymentsTestSupport {

    static final WireMockServer downstream = new WireMockServer(options().dynamicPort());

    static {
        downstream.start();
    }

    @DynamicPropertySource
    static void pointClientsAtWireMock(DynamicPropertyRegistry registry) {
        registry.add("spring.http.serviceclient.invoices.base-url", downstream::baseUrl);
        registry.add("spring.http.serviceclient.provider.base-url", downstream::baseUrl);
    }

    @Autowired MockMvc mvc;
    @Autowired PaymentLinkRepository links;
    @Autowired PaymentReportRepository reports;

    private final List<Long> invoiceIds = new ArrayList<>();

    @BeforeEach
    void resetWireMock() {
        downstream.resetAll();
    }

    @AfterEach
    void removeTestRows() {
        reports.findAll().stream().filter(r -> invoiceIds.contains(r.getInvoiceId())).forEach(reports::delete);
        invoiceIds.forEach(id -> links.findByInvoiceId(id).ifPresent(links::delete));
    }

    long newInvoiceId() {
        long id = ThreadLocalRandom.current().nextLong(5_000_000, 9_000_000);
        invoiceIds.add(id);
        return id;
    }

    static void stubInvoice(long id, String status, String total) {
        downstream.stubFor(get(urlEqualTo("/api/v1/invoices/" + id)).willReturn(aResponse()
                .withStatus(200).withHeader("Content-Type", "application/json")
                .withBody(invoiceJson(id, status, total))));
    }

    static void stubMarkPaid(long id, String total) {
        downstream.stubFor(put(urlEqualTo("/api/v1/invoices/" + id + "/status"))
                .withRequestBody(equalToJson("{\"status\":\"PAID\"}"))
                .willReturn(aResponse().withStatus(200).withHeader("Content-Type", "application/json")
                        .withBody(invoiceJson(id, "PAID", total))));
    }

    static void stubCheckoutCreated() {
        downstream.stubFor(post(urlEqualTo("/checkouts")).willReturn(aResponse()
                .withStatus(201).withHeader("Content-Type", "application/json")
                .withBody("{\"checkoutId\":\"chk-{{jsonPath request.body '$.reference'}}\","
                        + "\"url\":\"https://pay.example.com/chk-{{jsonPath request.body '$.reference'}}\"}")
                .withTransformers("response-template")));
    }

    static String invoiceJson(long id, String status, String total) {
        return """
                {"id":%d,"number":"INV-%d","customer":"Harbor Bakery","description":"40 hours of bookkeeping, October",
                 "total":%s,"currency":"USD","status":"%s","createdAt":"2026-11-23T09:00:00Z"}""".formatted(id, id, total, status);
    }

    ResultActions requestLink(long invoiceId) throws Exception {
        return mvc.perform(MockMvcRequestBuilders.post("/api/v1/payment-links")
                .contentType(MediaType.APPLICATION_JSON).content("{\"invoiceId\":" + invoiceId + "}"));
    }

    ResultActions sendReport(String eventId, String checkoutId, String status, long amountCents) throws Exception {
        return mvc.perform(MockMvcRequestBuilders.post("/api/v1/payment-reports")
                .contentType(MediaType.APPLICATION_JSON).content("""
                        {"eventId":"%s","checkoutId":"%s","status":"%s","amountCents":%d}"""
                        .formatted(eventId, checkoutId, status, amountCents)));
    }
}
