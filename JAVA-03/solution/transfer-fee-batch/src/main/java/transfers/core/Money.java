package transfers.core;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Money rounding used by every rule: two decimal places, half up. */
public final class Money {
    private Money() {}

    public static BigDecimal cents(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP);
    }
}
