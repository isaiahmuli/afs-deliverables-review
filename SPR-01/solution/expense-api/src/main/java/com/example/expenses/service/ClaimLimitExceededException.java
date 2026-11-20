package com.example.expenses.service;

import java.math.BigDecimal;

public class ClaimLimitExceededException extends RuntimeException {

    private final BigDecimal claimAmount;
    private final BigDecimal maxClaimAmount;

    public ClaimLimitExceededException(BigDecimal claimAmount, BigDecimal maxClaimAmount) {
        super("The claim of %s USD exceeds this team's limit of %s USD per claim."
                .formatted(claimAmount.toPlainString(), maxClaimAmount.toPlainString()));
        this.claimAmount = claimAmount;
        this.maxClaimAmount = maxClaimAmount;
    }

    public BigDecimal claimAmount() {
        return claimAmount;
    }

    public BigDecimal maxClaimAmount() {
        return maxClaimAmount;
    }
}
