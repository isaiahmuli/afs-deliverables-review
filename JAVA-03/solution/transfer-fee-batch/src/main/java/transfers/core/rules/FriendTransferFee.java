package transfers.core.rules;

import java.math.BigDecimal;
import transfers.core.FeeRule;

/** To a friend in the app: free. */
public final class FriendTransferFee implements FeeRule {
    @Override public String type() { return "friend"; }
    @Override public BigDecimal feeUsd(BigDecimal amountUsd) { return BigDecimal.ZERO; }
}
