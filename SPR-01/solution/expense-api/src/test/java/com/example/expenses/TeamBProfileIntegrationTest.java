package com.example.expenses;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.expenses.config.ExpenseLimitsProperties;
import java.math.BigDecimal;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.client.RestTestClient;

/** The same application with the team-b profile: the limit drops from $500 to $100. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("team-b")
class TeamBProfileIntegrationTest {

    @LocalServerPort
    int port;

    @Autowired
    ExpenseLimitsProperties limits;

    @Test
    void profileOverridesTheBaseLimit() {
        assertThat(limits.maxClaimAmount()).isEqualByComparingTo("100.00");
    }

    @Test
    void claimOf150IsRefusedUnderTheTeamBLimitAndNotSaved() {
        RestTestClient http = RestTestClient.bindToServer().baseUrl("http://localhost:" + port).build();

        http.post().uri("/api/expenses").contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("employee", "Amina Otieno", "description", "Hotel night",
                        "expenseDate", "2026-11-18", "receiptTotal", new BigDecimal("150.00"), "claimAmount", new BigDecimal("150.00")))
                .exchange()
                .expectStatus().isEqualTo(422)
                .expectBody()
                .jsonPath("$.title").isEqualTo("Claim exceeds team limit")
                .jsonPath("$.detail").isEqualTo("The claim of 150.00 USD exceeds this team's limit of 100.00 USD per claim.");

        http.get().uri("/api/expenses").exchange().expectStatus().isOk().expectBody().json("[]");
    }
}
