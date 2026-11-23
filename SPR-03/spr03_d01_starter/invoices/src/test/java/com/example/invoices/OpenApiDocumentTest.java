package com.example.invoices;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.invoices.support.OpenApiResponseValidator;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import tools.jackson.databind.JsonNode;

/** D1-4: springdoc serves an OpenAPI 3.1 document, and the validator really rejects a wrong body. */
@SpringBootTest
@AutoConfigureMockMvc
class OpenApiDocumentTest extends InvoiceApiTestSupport {

    @Test
    void servesOpenApi31WithTheVersionedPaths() throws Exception {
        JsonNode doc = body(mvc.perform(get("/v3/api-docs")).andExpect(status().isOk()));
        assertThat(doc.get("openapi").asString()).startsWith("3.1");
        assertThat(doc.get("paths").propertyNames())
                .contains("/api/v1/invoices", "/api/v1/invoices/{id}", "/api/v1/invoices/{id}/status");
        assertThat(doc.get("components").get("schemas").get("Problem").get("required").toString())
                .contains("type", "title", "status", "detail");
    }

    /** Guards the guard: a body missing required fields or with a bad status must fail validation. */
    @Test
    void theValidatorRejectsBodiesThatDoNotMatch() throws Exception {
        String doc = OpenApiResponseValidator.fetchDocument(mvc);
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> OpenApiResponseValidator.assertMatches(doc, "Invoice",
                        "{\"id\":1001,\"customer\":\"Harbor Bakery\",\"status\":\"SHIPPED\"}"))
                .isInstanceOf(AssertionError.class)
                .hasMessageContaining("/status: does not have a value in the enumeration")
                .hasMessageContaining("required property 'total' not found");
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> OpenApiResponseValidator.assertMatches(doc, "Problem",
                        "{\"status\":404}"))
                .isInstanceOf(AssertionError.class)
                .hasMessageContaining("required property 'detail' not found");
    }
}
