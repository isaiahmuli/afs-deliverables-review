package transfers.core;

import java.math.BigDecimal;

/**
 * The fee for one transfer type (a Strategy). A new transfer type is one new class
 * implementing this interface; no existing rule or the calculator changes.
 */
public interface FeeRule {
    /** The type as it appears in the input, e.g. {@code bank}. */
    String type();

    /** The fee in US dollars, before rounding to cents. */
    BigDecimal feeUsd(BigDecimal amountUsd);

    /** True if the recipient is paid in their own currency at the night's rate. */
    default boolean paysOutInRecipientCurrency() {
        return false;
    }
}
