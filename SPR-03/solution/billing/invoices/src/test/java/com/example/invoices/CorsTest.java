package com.example.invoices;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;

/** D1-7: only the billing web app's origin may call the API from a browser. */
@SpringBootTest
@AutoConfigureMockMvc
class CorsTest extends InvoiceApiTestSupport {

    static final String BILLING = "https://billing.example.com";

    @Test
    void preflightFromTheBillingAppGetsTheMethodsAndHeadersItNeeds() throws Exception {
        mvc.perform(options("/api/v1/invoices")
                        .header("Origin", BILLING)
                        .header("Access-Control-Request-Method", "POST")
                        .header("Access-Control-Request-Headers", "Content-Type, Idempotency-Key"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", BILLING))
                .andExpect(header().string("Access-Control-Allow-Methods", "GET,POST,PUT,DELETE"))
                .andExpect(header().string("Access-Control-Allow-Headers", "Content-Type, Idempotency-Key"))
                .andExpect(header().string("Access-Control-Max-Age", "3600"));

        mvc.perform(options("/api/v1/invoices/1001/status")
                        .header("Origin", BILLING).header("Access-Control-Request-Method", "PUT"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", BILLING));
    }

    @Test
    void preflightFromAnUnlistedOriginIsRejected() throws Exception {
        mvc.perform(options("/api/v1/invoices")
                        .header("Origin", "https://evil.example.com")
                        .header("Access-Control-Request-Method", "POST"))
                .andExpect(status().isForbidden())
                .andExpect(header().doesNotExist("Access-Control-Allow-Origin"))
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value("https://billing.example.com/problems/cors-rejected"))
                .andExpect(jsonPath("$.detail").value("This origin is not allowed to call the API from a browser."));
    }

    @Test
    void aMethodTheAppDoesNotUseIsRejected() throws Exception {
        mvc.perform(options("/api/v1/invoices")
                        .header("Origin", BILLING).header("Access-Control-Request-Method", "PATCH"))
                .andExpect(status().isForbidden());
    }

    @Test
    void actualRequestsFromTheBillingAppCanReadTheLocationHeader() throws Exception {
        mvc.perform(get("/api/v1/invoices").header("Origin", BILLING))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", BILLING))
                .andExpect(header().string("Access-Control-Expose-Headers", "Location, Idempotent-Replayed"));
    }
}
