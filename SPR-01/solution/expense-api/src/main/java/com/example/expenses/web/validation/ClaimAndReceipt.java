package com.example.expenses.web.validation;

import java.math.BigDecimal;

/** Anything that carries a claim and the receipt total it is made against. */
public interface ClaimAndReceipt {

    BigDecimal claimAmount();

    BigDecimal receiptTotal();
}
