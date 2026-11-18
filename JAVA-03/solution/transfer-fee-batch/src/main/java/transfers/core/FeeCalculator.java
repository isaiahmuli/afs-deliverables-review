package transfers.core;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Prices transfers (the Strategy context). It holds one {@link FeeRule} per type by
 * composition and picks the rule by the transfer's type; it never names a type itself.
 * Usable by the batch and by a future website alike: it knows nothing about files,
 * threads, or receipts.
 */
public final class FeeCalculator {
    private final Map<String, FeeRule> rulesByType = new LinkedHashMap<>();
    private final RateSource rates;

    public FeeCalculator(Collection<? extends FeeRule> rules, RateSource rates) {
        for (FeeRule rule : rules) {
            if (rulesByType.putIfAbsent(rule.type(), rule) != null) {
                throw new IllegalArgumentException("Two fee rules for type: " + rule.type());
            }
        }
        this.rates = Objects.requireNonNull(rates, "rates");
    }

    /**
     * @throws UnsupportedTransferTypeException for a type with no rule
     * @throws UnknownCurrencyException if an international transfer's currency has no rate
     */
    public Quote price(Transfer transfer) {
        FeeRule rule = rulesByType.get(transfer.type());
        if (rule == null) {
            throw new UnsupportedTransferTypeException(transfer.id(), transfer.type());
        }
        BigDecimal fee = Money.cents(rule.feeUsd(transfer.amountUsd()));
        if (!rule.paysOutInRecipientCurrency()) {
            return new Quote(transfer, fee, Money.cents(transfer.amountUsd()), "USD");
        }
        String currency = transfer.recipientCurrency();
        BigDecimal received = transfer.amountUsd().multiply(rates.unitsPerUsd(currency));
        return new Quote(transfer, fee, Money.cents(received), currency);
    }

    public Set<String> supportedTypes() {
        return Set.copyOf(rulesByType.keySet());
    }
}
