package com.example.invoices;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.invoices.support.OpenApiResponseValidator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/** Shared helpers. Tests run against the Compose PostgreSQL and leave their invoices behind. */
abstract class InvoiceApiTestSupport {

    static final String HARBOR = """
            {"customer":"Harbor Bakery","description":"40 hours of bookkeeping, October","total":1200.00}""";
    static final String FLORIST = """
            {"customer":"Northside Florist","description":"Quarterly payroll filing","total":340.00}""";

    @Autowired MockMvc mvc;
    @Autowired JsonMapper json;
    @Autowired InvoiceRepository invoices;

    private static String openApiDocument;

    ResultActions postInvoice(String body) throws Exception {
        return mvc.perform(post("/api/v1/invoices").contentType(MediaType.APPLICATION_JSON).content(body));
    }

    long createInvoice(String body) throws Exception {
        return id(postInvoice(body).andExpect(status().isCreated()));
    }

    long createSentInvoice(String body) throws Exception {
        long id = createInvoice(body);
        setStatus(id, "SENT").andExpect(status().isOk());
        return id;
    }

    ResultActions setStatus(long id, String status) throws Exception {
        return mvc.perform(put("/api/v1/invoices/{id}/status", id)
                .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"" + status + "\"}"));
    }

    long id(ResultActions result) throws Exception {
        return body(result).get("id").asLong();
    }

    JsonNode body(ResultActions result) throws Exception {
        return json.readTree(result.andReturn().getResponse().getContentAsString());
    }

    /** Checks a real response body against the named schema in the service's OpenAPI document. */
    void assertMatchesSchema(String schema, ResultActions result) throws Exception {
        if (openApiDocument == null) {
            openApiDocument = OpenApiResponseValidator.fetchDocument(mvc);
        }
        OpenApiResponseValidator.assertMatches(openApiDocument, schema,
                result.andReturn().getResponse().getContentAsString());
    }
}
