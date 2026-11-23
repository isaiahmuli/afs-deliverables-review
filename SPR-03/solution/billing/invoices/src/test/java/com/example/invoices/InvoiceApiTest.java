package com.example.invoices;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

/** D1-1 methods and status codes, D1-2 error contract, D1-4 response validation, business rules. */
@SpringBootTest
@AutoConfigureMockMvc
class InvoiceApiTest extends InvoiceApiTestSupport {

    // --- create and view (D1-1) ---

    @Test
    void createReturns201WithLocationAndADraftThatMatchesTheSchema() throws Exception {
        ResultActions created = postInvoice(HARBOR)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andExpect(jsonPath("$.customer").value("Harbor Bakery"))
                .andExpect(jsonPath("$.description").value("40 hours of bookkeeping, October"))
                .andExpect(jsonPath("$.total").value(1200.00))
                .andExpect(jsonPath("$.currency").value("USD"));
        long id = id(created);
        created.andExpect(header().string("Location", endsWith("/api/v1/invoices/" + id)))
                .andExpect(jsonPath("$.number").value("INV-" + id));
        assertMatchesSchema("Invoice", created);

        String location = created.andReturn().getResponse().getHeader("Location");
        ResultActions viewed = mvc.perform(get(location)).andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id));
        assertMatchesSchema("Invoice", viewed);
    }

    @Test
    void listShowsInvoicesInOrder() throws Exception {
        long first = createInvoice(HARBOR);
        long second = createInvoice(FLORIST);
        ResultActions listed = mvc.perform(get("/api/v1/invoices")).andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == " + second + ")].customer").value("Northside Florist"));
        var ids = new java.util.ArrayList<Long>();
        body(listed).forEach(invoice -> ids.add(invoice.get("id").asLong()));
        assertThat(ids).containsSubsequence(first, second).isSorted();
    }

    @Test
    void unknownInvoiceIs404ProblemJson() throws Exception {
        ResultActions missing = mvc.perform(get("/api/v1/invoices/{id}", 9_999_999))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value("https://billing.example.com/problems/not-found"))
                .andExpect(jsonPath("$.title").value("Not found"))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.detail").value("Invoice INV-9999999 does not exist."));
        assertMatchesSchema("Problem", missing);
    }

    // --- validation (D1-2) ---

    @Test
    void incompleteInvoiceIsRefusedNamingEveryField() throws Exception {
        long before = invoices.count();
        ResultActions refused = postInvoice("{\"customer\":\"  \",\"total\":0}")
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value("https://billing.example.com/problems/invalid-request"))
                .andExpect(jsonPath("$.title").value("Invalid request"))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.detail").value(
                        "Fix these fields: customer is required; description is required; total must be above zero."))
                .andExpect(jsonPath("$.errors", hasSize(3)))
                .andExpect(jsonPath("$.errors[2].field").value("total"));
        assertMatchesSchema("Problem", refused);
        assertThat(invoices.count()).isEqualTo(before);
    }

    @Test
    void totalWithThreeDecimalsOrNegativeIsRefused() throws Exception {
        postInvoice("{\"customer\":\"Harbor Bakery\",\"description\":\"x\",\"total\":12.345}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Fix these fields: total must have at most two decimal places."));
        postInvoice("{\"customer\":\"Harbor Bakery\",\"description\":\"x\",\"total\":-5}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Fix these fields: total must be above zero."));
    }

    @Test
    void malformedJsonAndWrongTypesAreRefusedWithoutInternals() throws Exception {
        postInvoice("{\"customer\":").andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.detail").value("The request body is missing or is not valid JSON."));
        postInvoice("{\"customer\":\"a\",\"description\":\"b\",\"total\":\"lots\"}").andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("total has a value of the wrong type."));
        mvc.perform(get("/api/v1/invoices/abc")).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type").value("https://billing.example.com/problems/invalid-request"))
                .andExpect(jsonPath("$.detail").value("id must be a whole number."));
        setStatus(createInvoice(HARBOR), "SHIPPED").andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("status must be one of [DRAFT, SENT, PAID, CANCELLED]."));
    }

    @Test
    void springsOwnRefusalsUseTheSameFormat() throws Exception {
        mvc.perform(post("/api/v1/invoices").contentType(MediaType.TEXT_PLAIN).content("x"))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value("https://billing.example.com/problems/invalid-request"))
                .andExpect(jsonPath("$.title").exists())
                .andExpect(jsonPath("$.detail").exists());
        mvc.perform(delete("/api/v1/invoices")).andExpect(status().isMethodNotAllowed())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value("https://billing.example.com/problems/invalid-request"));
        mvc.perform(get("/api/v2/invoices")).andExpect(status().isNotFound())
                .andExpect(jsonPath("$.type").value("https://billing.example.com/problems/not-found"));
    }

    // --- status changes (business rules, D1-1 409, D1-3 repeated changes) ---

    @Test
    void draftIsSentThenPaid() throws Exception {
        long id = createInvoice(HARBOR);
        ResultActions sent = setStatus(id, "SENT").andExpect(status().isOk()).andExpect(jsonPath("$.status").value("SENT"));
        assertMatchesSchema("Invoice", sent);
        setStatus(id, "PAID").andExpect(status().isOk()).andExpect(jsonPath("$.status").value("PAID"));
        mvc.perform(get("/api/v1/invoices/{id}", id)).andExpect(jsonPath("$.status").value("PAID"));
    }

    @Test
    void settingTheSameStatusAgainSucceedsAndChangesNothing() throws Exception {
        long id = createSentInvoice(HARBOR);
        String before = body(mvc.perform(get("/api/v1/invoices/{id}", id))).toString();
        setStatus(id, "SENT").andExpect(status().isOk()).andExpect(jsonPath("$.status").value("SENT"));
        setStatus(id, "SENT").andExpect(status().isOk());
        assertThat(body(mvc.perform(get("/api/v1/invoices/{id}", id))).toString()).isEqualTo(before);

        setStatus(id, "PAID").andExpect(status().isOk());
        setStatus(id, "PAID").andExpect(status().isOk()).andExpect(jsonPath("$.status").value("PAID"));
    }

    @Test
    void draftOrSentInvoicesCanBeCancelled() throws Exception {
        setStatus(createInvoice(FLORIST), "CANCELLED").andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
        setStatus(createSentInvoice(FLORIST), "CANCELLED").andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
    }

    @Test
    void forbiddenStatusChangesAre409() throws Exception {
        long paid = createSentInvoice(HARBOR);
        setStatus(paid, "PAID").andExpect(status().isOk());
        ResultActions refused = setStatus(paid, "CANCELLED").andExpect(status().isConflict())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value("https://billing.example.com/problems/invalid-state"))
                .andExpect(jsonPath("$.detail").value("Invoice INV-" + paid + " is PAID and cannot become CANCELLED."));
        assertMatchesSchema("Problem", refused);

        long draft = createInvoice(HARBOR);
        setStatus(draft, "PAID").andExpect(status().isConflict());
        long cancelled = createInvoice(HARBOR);
        setStatus(cancelled, "CANCELLED").andExpect(status().isOk());
        setStatus(cancelled, "SENT").andExpect(status().isConflict());
        setStatus(9_999_999, "SENT").andExpect(status().isNotFound());
    }

    // --- delete (D1-1 204, D1-3 repeated deletes) ---

    @Test
    void deletingADraftIs204AndASecondDeleteChangesNothing() throws Exception {
        long id = createInvoice(FLORIST);
        long before = invoices.count();
        mvc.perform(delete("/api/v1/invoices/{id}", id)).andExpect(status().isNoContent())
                .andExpect(content().string(""));
        mvc.perform(get("/api/v1/invoices/{id}", id)).andExpect(status().isNotFound());
        mvc.perform(delete("/api/v1/invoices/{id}", id)).andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON));
        assertThat(invoices.count()).isEqualTo(before - 1);
    }

    @Test
    void onlyDraftsCanBeDeleted() throws Exception {
        long sent = createSentInvoice(HARBOR);
        mvc.perform(delete("/api/v1/invoices/{id}", sent)).andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("Invoice INV-" + sent + " is SENT; only draft invoices can be deleted."));
        long cancelled = createInvoice(HARBOR);
        setStatus(cancelled, "CANCELLED");
        mvc.perform(delete("/api/v1/invoices/{id}", cancelled)).andExpect(status().isConflict());
        assertThat(invoices.findById(sent)).isPresent();
    }
}
