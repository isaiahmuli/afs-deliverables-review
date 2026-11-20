package com.example.expenses.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.expenses.config.ExpenseLimitsProperties;
import com.example.expenses.domain.NewExpense;
import com.example.expenses.repository.InMemoryExpenseRepository;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

class ExpenseServiceTest {

    private final InMemoryExpenseRepository repository = new InMemoryExpenseRepository();
    private final Clock clock = Clock.fixed(Instant.parse("2026-11-19T08:00:00Z"), ZoneOffset.UTC);

    private ExpenseService serviceWithLimit(String limit) {
        return new ExpenseService(repository, new ExpenseLimitsProperties(new BigDecimal(limit)), clock);
    }

    private static NewExpense claim(String receipt, String claim) {
        return new NewExpense("Amina", "Hotel", LocalDate.parse("2026-11-18"),
                new BigDecimal(receipt), new BigDecimal(claim));
    }

    @Test
    void claimAtTheLimitIsSaved() {
        var saved = serviceWithLimit("500.00").submit(claim("150.00", "150.00"));

        assertThat(saved.id()).isEqualTo(1);
        assertThat(saved.submittedAt()).isEqualTo(clock.instant());
        assertThat(repository.findAll()).containsExactly(saved);
        assertThat(serviceWithLimit("150").submit(claim("150.00", "150.00")).id()).isEqualTo(2);
    }

    @Test
    void claimOverTheLimitIsRefusedAndNotSaved() {
        assertThatThrownBy(() -> serviceWithLimit("100.00").submit(claim("150.00", "150.00")))
                .isInstanceOf(ClaimLimitExceededException.class)
                .hasMessage("The claim of 150.00 USD exceeds this team's limit of 100.00 USD per claim.");

        assertThat(repository.findAll()).isEmpty();
    }

    @Test
    void unknownIdIsNotFound() {
        assertThatThrownBy(() -> serviceWithLimit("500").findById(42))
                .isInstanceOf(ExpenseNotFoundException.class)
                .hasMessage("No expense with id 42.");
    }
}
