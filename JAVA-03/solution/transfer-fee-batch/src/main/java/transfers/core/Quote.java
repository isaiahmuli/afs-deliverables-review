package transfers.core;

import java.math.BigDecimal;

/** A priced transfer: the fee the sender pays and what the recipient receives. */
public record Quote(Transfer transfer, BigDecimal feeUsd, BigDecimal recipientAmount, String recipientCurrency) {
}
