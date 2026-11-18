package transfers.core;

import java.math.BigDecimal;
import java.util.Objects;

/** One transfer to price. The amount is always US dollars. */
public record Transfer(String id, String type, String sender, BigDecimal amountUsd, String recipientCurrency) {
    public Transfer {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(sender, "sender");
        Objects.requireNonNull(amountUsd, "amountUsd");
        Objects.requireNonNull(recipientCurrency, "recipientCurrency");
        if (amountUsd.signum() <= 0) {
            throw new IllegalArgumentException("Transfer " + id + " amount must be positive: " + amountUsd);
        }
    }
}
