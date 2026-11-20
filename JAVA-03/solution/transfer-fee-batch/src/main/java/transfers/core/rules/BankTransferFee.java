package transfers.core.rules;

import java.math.BigDecimal;
import transfers.core.FeeRule;

/** To a bank account: $0.50. */
public final class BankTransferFee implements FeeRule {
    private static final BigDecimal FEE = new BigDecimal("0.50");

    @Override public String type() { return "bank"; }
    @Override public BigDecimal feeUsd(BigDecimal amountUsd) { return FEE; }
}
