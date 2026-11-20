package transfers.core.rules;

import java.math.BigDecimal;
import transfers.core.FeeRule;

/** Bill payment: $0.25. Added as one new class; no other rule changed. Input type: {@code bill}. */
public final class BillPaymentFee implements FeeRule {
    private static final BigDecimal FEE = new BigDecimal("0.25");

    @Override public String type() { return "bill"; }
    @Override public BigDecimal feeUsd(BigDecimal amountUsd) { return FEE; }
}
