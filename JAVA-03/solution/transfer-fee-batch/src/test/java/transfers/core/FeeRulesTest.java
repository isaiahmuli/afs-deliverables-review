package transfers.core;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import transfers.core.rules.BankTransferFee;
import transfers.core.rules.BillPaymentFee;
import transfers.core.rules.FriendTransferFee;
import transfers.core.rules.InternationalTransferFee;

/** The fee table from the deliverable, plus rounding and minimum boundaries. */
class FeeRulesTest {
    private static final RateSource RATES = currency -> switch (currency) {
        case "MXN" -> new BigDecimal("17.25");
        case "EUR" -> new BigDecimal("0.92");
        default -> throw new UnknownCurrencyException(currency);
    };
    private static final FeeCalculator CALCULATOR = new FeeCalculator(List.of(new FriendTransferFee(),
            new BankTransferFee(), new BillPaymentFee(), new InternationalTransferFee()), RATES);

    @ParameterizedTest(name = "{0} USD {1} -> fee {3}, recipient {4} {5}")
    @CsvSource({
            "friend,        25.00,  USD, 0.00,  25.00,   USD",
            "bank,          80.00,  USD, 0.50,  80.00,   USD",
            "bill,          120.00, USD, 0.25,  120.00,  USD",
            "international, 100.00, EUR, 3.00,  92.00,   EUR",   // 2% = 2.00, minimum 3
            "international, 150.00, EUR, 3.00,  138.00,  EUR",   // 2% = 3.00 exactly
            "international, 150.25, EUR, 3.01,  138.23,  EUR",   // 3.005 rounds half up; 138.23
            "international, 500.00, EUR, 10.00, 460.00,  EUR",
            "international, 200.00, MXN, 4.00,  3450.00, MXN",   // Mexico: full 200 converted
            "international, 0.01,   MXN, 3.00,  0.17,    MXN"    // 0.1725 rounds to 0.17
    })
    void feesAndRecipientAmounts(String type, String amount, String currency,
                                 String fee, String received, String receivedCurrency) {
        var quote = CALCULATOR.price(new Transfer("T1", type, "s", new BigDecimal(amount), currency));
        assertEquals(new BigDecimal(fee), quote.feeUsd());
        assertEquals(new BigDecimal(received), quote.recipientAmount());
        assertEquals(receivedCurrency, quote.recipientCurrency());
    }
}
