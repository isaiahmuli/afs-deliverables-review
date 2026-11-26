package com.example.invoices;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;

/** D1-3: replayed creates, a reused key, and new requests with identical details. */
@SpringBootTest
@AutoConfigureMockMvc
class IdempotencyTest extends InvoiceApiTestSupport {

    private ResultActions postWithKey(String key, String body) throws Exception {
        return mvc.perform(post("/api/v1/invoices").header("Idempotency-Key", key)
                .contentType(MediaType.APPLICATION_JSON).content(body));
    }

    @Test
    void replayingACreateWithTheSameKeyReturnsTheOriginalResponseAndCreatesNothing() throws Exception {
        String key = UUID.randomUUID().toString();
        MvcResult first = postWithKey(key, HARBOR).andExpect(status().isCreated()).andReturn();
        long count = invoices.count();

        // Same content, written differently (1200 vs 1200.00): still the same request.
        String sameContent = """
                {"total":1200,"customer":"Harbor Bakery","description":"40 hours of bookkeeping, October"}""";
        MvcResult replay = postWithKey(key, sameContent)
                .andExpect(status().isCreated())
                .andExpect(header().string("Idempotent-Replayed", "true"))
                .andExpect(header().string("Location", first.getResponse().getHeader("Location")))
                .andReturn();

        assertThat(replay.getResponse().getContentAsString()).isEqualTo(first.getResponse().getContentAsString());
        assertThat(invoices.count()).isEqualTo(count);
        System.out.printf("[idempotency] replay with key %s returned the original invoice %s; invoices before %d, after %d%n",
                key.substring(0, 8), json.readTree(first.getResponse().getContentAsString()).get("number").asString(),
                count, invoices.count());
    }

    @Test
    void theReplayIsTheOriginalResponseEvenAfterTheInvoiceChanged() throws Exception {
        String key = UUID.randomUUID().toString();
        long id = id(postWithKey(key, HARBOR).andExpect(status().isCreated()));
        setStatus(id, "SENT");
        postWithKey(key, HARBOR).andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.status").value("DRAFT"));
    }

    @Test
    void theSameKeyWithDifferentContentIsRefusedAndCreatesNothing() throws Exception {
        String key = UUID.randomUUID().toString();
        postWithKey(key, HARBOR).andExpect(status().isCreated());
        long count = invoices.count();

        ResultActions refused = postWithKey(key, FLORIST)
                .andExpect(status().isUnprocessableContent())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value("https://billing.example.com/problems/idempotency-key-reused"))
                .andExpect(jsonPath("$.title").value("Idempotency key already used"))
                .andExpect(jsonPath("$.status").value(422))
                .andExpect(jsonPath("$.detail").value(
                        "This Idempotency-Key was already used for a different invoice. Use a new key for a new request."));
        assertMatchesSchema("Problem", refused);
        assertThat(invoices.count()).isEqualTo(count);
    }

    @Test
    void aNewRequestWithIdenticalDetailsIsProcessedNormally() throws Exception {
        long count = invoices.count();
        long withoutKey1 = createInvoice(HARBOR);
        long withoutKey2 = createInvoice(HARBOR);
        long withKeyA = id(postWithKey(UUID.randomUUID().toString(), HARBOR).andExpect(status().isCreated()));
        long withKeyB = id(postWithKey(UUID.randomUUID().toString(), HARBOR).andExpect(status().isCreated()));
        assertThat(List.of(withoutKey1, withoutKey2, withKeyA, withKeyB)).doesNotHaveDuplicates();
        assertThat(invoices.count()).isEqualTo(count + 4);
    }

    @Test
    void aBlankKeyIsRefused() throws Exception {
        postWithKey(" ", HARBOR).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Idempotency-Key must be 1 to 255 characters."));
    }

    /** A request sent twice by mistake can arrive twice at the same moment: still one invoice. */
    @Test
    void twoSimultaneousRequestsWithOneKeyCreateOneInvoice() throws Exception {
        String key = UUID.randomUUID().toString();
        long count = invoices.count();
        CountDownLatch start = new CountDownLatch(1);
        List<Future<MvcResult>> results = new ArrayList<>();
        try (ExecutorService pool = Executors.newFixedThreadPool(4)) {
            for (int i = 0; i < 4; i++) {
                Callable<MvcResult> call = () -> {
                    start.await();
                    return postWithKey(key, HARBOR).andReturn();
                };
                results.add(pool.submit(call));
            }
            start.countDown();
            List<String> bodies = new ArrayList<>();
            for (Future<MvcResult> result : results) {
                assertThat(result.get().getResponse().getStatus()).isEqualTo(201);
                bodies.add(result.get().getResponse().getContentAsString());
            }
            assertThat(bodies).containsOnly(bodies.getFirst());
        }
        assertThat(invoices.count()).isEqualTo(count + 1);
    }
}
