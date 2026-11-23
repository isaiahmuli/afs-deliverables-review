package com.example.payments;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.payments.support.OpenApiResponseValidator;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

/** D1-7 for payments, and its OpenAPI 3.1 document (validating payments responses is optional). */
class CorsAndOpenApiTest extends PaymentsTestSupport {

    @Test
    void preflightFromTheBillingAppIsAllowed() throws Exception {
        mvc.perform(options("/api/v1/payment-links")
                        .header("Origin", "https://billing.example.com")
                        .header("Access-Control-Request-Method", "POST")
                        .header("Access-Control-Request-Headers", "Content-Type"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "https://billing.example.com"))
                .andExpect(header().string("Access-Control-Allow-Methods", "GET,POST"))
                .andExpect(header().string("Access-Control-Allow-Headers", "Content-Type"));
    }

    @Test
    void preflightFromAnUnlistedOriginIsRejected() throws Exception {
        mvc.perform(options("/api/v1/payment-links")
                        .header("Origin", "https://evil.example.com")
                        .header("Access-Control-Request-Method", "POST"))
                .andExpect(status().isForbidden())
                .andExpect(header().doesNotExist("Access-Control-Allow-Origin"))
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value("https://billing.example.com/problems/cors-rejected"));
    }

    @Test
    void servesOpenApi31AndALinkMatchesItsSchema() throws Exception {
        String doc = OpenApiResponseValidator.fetchDocument(mvc);
        assertThat(doc).contains("\"openapi\":\"3.1");

        long id = newInvoiceId();
        stubInvoice(id, "SENT", "1200.00");
        stubCheckoutCreated();
        String link = requestLink(id).andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        OpenApiResponseValidator.assertMatches(doc, "PaymentLink", link);
        String problem = mvc.perform(get("/api/v1/payment-links/{id}", 1)).andExpect(status().isNotFound())
                .andReturn().getResponse().getContentAsString();
        OpenApiResponseValidator.assertMatches(doc, "Problem", problem);
    }
}
