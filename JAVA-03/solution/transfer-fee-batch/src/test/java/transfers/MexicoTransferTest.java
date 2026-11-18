package transfers;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import transfers.core.FeeCalculator;
import transfers.core.Transfer;
import transfers.core.rules.InternationalTransferFee;
import transfers.rates.BankRates;

/**
 * Reproduces the Mexico failure: a $200 transfer to Mexico costs $4 and delivers MXN 3450.00.
 * Failed on the original code (evidence/01_before_fix_tests.txt); the call changed with the
 * restructure, the expected values did not.
 */
class MexicoTransferTest {
    @Test
    void twoHundredDollarsToMexico() {
        var calculator = new FeeCalculator(List.of(new InternationalTransferFee()),
                BankRates.getInstance(Path.of("bank-rates.csv")));
        var transfer = new Transfer("T0005", "international", "sender-0005", new BigDecimal("200.00"), "MXN");

        var quote = calculator.price(transfer);

        assertEquals(new BigDecimal("4.00"), quote.feeUsd());
        assertEquals(new BigDecimal("3450.00"), quote.recipientAmount());
        assertEquals("MXN", quote.recipientCurrency());
    }
}
