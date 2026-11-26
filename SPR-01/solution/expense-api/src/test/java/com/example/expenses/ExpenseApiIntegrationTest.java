package com.example.expenses;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.expenses.web.ExpenseResponse;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.client.RestTestClient;

/** The whole application on a random port, over real HTTP, with the base limit of $500. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ExpenseApiIntegrationTest {

    @LocalServerPort
    int port;

    RestTestClient http;

    @BeforeEach
    void client() {
        http = RestTestClient.bindToServer().baseUrl("http://localhost:" + port).build();
    }

    private static Map<String, Object> submission(String receipt, String claim) {
        return Map.of("employee", "Amina Otieno", "description", "Fuel for client visit",
                "expenseDate", "2026-11-18", "receiptTotal", new BigDecimal(receipt),
                "claimAmount", new BigDecimal(claim));
    }

    private List<ExpenseResponse> all() {
        return http.get().uri("/api/expenses").exchange()
                .expectStatus().isOk()
                .expectBody(new ParameterizedTypeReference<List<ExpenseResponse>>() { })
                .returnResult().getResponseBody();
    }

    @Test
    void submitThenRetrieveThroughEveryLayer() {
        int before = all().size();

        var created = http.post().uri("/api/expenses").contentType(MediaType.APPLICATION_JSON)
                .body(submission("120.00", "50.00")).exchange()
                .expectStatus().isCreated()
                .expectBody(ExpenseResponse.class).returnResult();
        ExpenseResponse saved = created.getResponseBody();
        assertThat(created.getResponseHeaders().getLocation()).hasPath("/api/expenses/" + saved.id());

        ExpenseResponse fetched = http.get().uri("/api/expenses/{id}", saved.id()).exchange()
                .expectStatus().isOk()
                .expectBody(ExpenseResponse.class).returnResult().getResponseBody();

        assertThat(fetched).isEqualTo(saved);
        assertThat(fetched.claimAmount()).isEqualByComparingTo("50.00");
        assertThat(fetched.receiptTotal()).isEqualByComparingTo("120.00");
        assertThat(all()).hasSize(before + 1).contains(saved);
    }

    @Test
    void claimOf150IsAcceptedUnderTheBaseLimitOf500() {
        http.post().uri("/api/expenses").contentType(MediaType.APPLICATION_JSON)
                .body(submission("150.00", "150.00")).exchange()
                .expectStatus().isCreated();
    }

    @Test
    void refusedSubmissionsAreNotSaved() {
        int before = all().size();

        http.post().uri("/api/expenses").contentType(MediaType.APPLICATION_JSON)
                .body(submission("50.00", "80.00")).exchange()
                .expectStatus().isBadRequest()
                .expectBody().jsonPath("$.errors[0].field").isEqualTo("claimAmount");
        http.post().uri("/api/expenses").contentType(MediaType.APPLICATION_JSON)
                .body(Map.of()).exchange()
                .expectStatus().isBadRequest()
                .expectBody().jsonPath("$.errors.length()").isEqualTo(5);

        assertThat(all()).hasSize(before);
    }

    @Test
    void unknownIdIsNotFound() {
        http.get().uri("/api/expenses/999999").exchange().expectStatus().isNotFound();
    }

    @Test
    void healthIsUp() {
        http.get().uri("/actuator/health").exchange()
                .expectStatus().isOk()
                .expectBody().jsonPath("$.status").isEqualTo("UP");
    }

    @Test
    void requestMetricsReflectApiTraffic() {
        http.get().uri("/api/expenses").exchange().expectStatus().isOk();
        http.get().uri("/api/expenses").exchange().expectStatus().isOk();

        http.get().uri("/actuator/metrics/http.server.requests?tag=uri:/api/expenses&tag=method:GET").exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.measurements[?(@.statistic=='COUNT')].value")
                .value(counts -> assertThat(((List<?>) counts).getFirst())
                        .asInstanceOf(org.assertj.core.api.InstanceOfAssertFactories.DOUBLE)
                        .isGreaterThanOrEqualTo(2.0))
                .jsonPath("$.measurements[?(@.statistic=='TOTAL_TIME')]").exists();
    }

    @Test
    void onlyHealthAndMetricsAreExposed() {
        http.get().uri("/actuator").exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$._links.health").exists()
                .jsonPath("$._links.metrics").exists()
                .jsonPath("$._links.env").doesNotExist()
                .jsonPath("$._links.configprops").doesNotExist();

        for (String hidden : List.of("env", "configprops", "beans", "mappings", "loggers", "heapdump")) {
            http.get().uri("/actuator/" + hidden).exchange().expectStatus().isEqualTo(HttpStatus.NOT_FOUND);
        }
    }
}
