package com.example.expenses.config;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * The running team's claim limit. Set in application.properties and overridden per team by a
 * profile (for example application-team-b.properties).
 */
@Validated
@ConfigurationProperties(prefix = "expenses")
public record ExpenseLimitsProperties(@NotNull @Positive BigDecimal maxClaimAmount) {
}
