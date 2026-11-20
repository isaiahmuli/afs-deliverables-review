package transfers.core.rules;

import java.math.BigDecimal;
import transfers.core.FeeRule;

/** International: 2% with a $3 minimum. The recipient is paid in their currency. */
public final class InternationalTransferFee implements FeeRule {
    private static final BigDecimal RATE = new BigDecimal("0.02");
    private static final BigDecimal MINIMUM = new BigDecimal("3.00");

    @Override public String type() { return "international"; }
    @Override public BigDecimal feeUsd(BigDecimal amountUsd) { return amountUsd.multiply(RATE).max(MINIMUM); }
    @Override public boolean paysOutInRecipientCurrency() { return true; }
}
