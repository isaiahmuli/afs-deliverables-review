package transfers.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import transfers.core.rules.BankTransferFee;
import transfers.core.rules.BillPaymentFee;
import transfers.core.rules.FriendTransferFee;
import transfers.core.rules.InternationalTransferFee;

class FeeCalculatorTest {
    private static final RateSource NO_RATES = currency -> { throw new UnknownCurrencyException(currency); };

    private static List<FeeRule> standardRules() {
        return List.of(new FriendTransferFee(), new BankTransferFee(), new BillPaymentFee(), new InternationalTransferFee());
    }

    @Test
    void unsupportedTypesAreRefusedWithRowIdAndType() {
        var calculator = new FeeCalculator(standardRules(), NO_RATES);
        var error = assertThrows(UnsupportedTransferTypeException.class,
                () -> calculator.price(new Transfer("T0070", "intl", "s", new BigDecimal("10"), "EUR")));
        assertEquals("T0070", error.transferId());
        assertEquals("intl", error.type());
    }

    @Test
    void theContextGainsANewStrategyWithNoChange() {
        // A type the app does not offer yet: one new class, passed in. FeeCalculator and the
        // existing rules are untouched.
        FeeRule wallet = new FeeRule() {
            @Override public String type() { return "wallet"; }
            @Override public BigDecimal feeUsd(BigDecimal amountUsd) { return new BigDecimal("0.10"); }
        };
        var rules = new ArrayList<FeeRule>(standardRules());
        rules.add(wallet);
        var calculator = new FeeCalculator(rules, NO_RATES);

        assertEquals(Set.of("friend", "bank", "bill", "international", "wallet"), calculator.supportedTypes());
        assertEquals(new BigDecimal("0.10"),
                calculator.price(new Transfer("T9", "wallet", "s", new BigDecimal("10"), "USD")).feeUsd());
        assertEquals(new BigDecimal("0.50"),
                calculator.price(new Transfer("T8", "bank", "s", new BigDecimal("10"), "USD")).feeUsd());
    }

    @Test
    void twoRulesForOneTypeAreRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> new FeeCalculator(List.of(new BankTransferFee(), new BankTransferFee()), NO_RATES));
    }

    @Test
    void domesticTransfersNeverAskForARate() {
        var calculator = new FeeCalculator(standardRules(), NO_RATES);
        var quote = calculator.price(new Transfer("T2", "bank", "s", new BigDecimal("80.00"), "USD"));
        assertEquals(new BigDecimal("80.00"), quote.recipientAmount());
    }

    @Test
    void aMissingRateIsAClearErrorNotANullPointer() {
        var calculator = new FeeCalculator(standardRules(), NO_RATES);
        var error = assertThrows(UnknownCurrencyException.class,
                () -> calculator.price(new Transfer("T3", "international", "s", new BigDecimal("50"), "JPY")));
        assertEquals("No bank rate for currency: JPY", error.getMessage());
    }

    @Test
    void invalidTransfersAreRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> new Transfer("T4", "bank", "s", BigDecimal.ZERO, "USD"));
        assertThrows(NullPointerException.class,
                () -> new Transfer("T5", null, "s", BigDecimal.ONE, "USD"));
    }
}
