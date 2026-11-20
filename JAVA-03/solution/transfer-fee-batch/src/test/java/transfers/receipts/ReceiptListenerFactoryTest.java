package transfers.receipts;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import transfers.core.Quote;
import transfers.core.Transfer;

class ReceiptListenerFactoryTest {
    private final List<String> texts = new ArrayList<>();
    private final SmsService fakeSms = (recipient, body) -> {
        texts.add(recipient + ": " + body);
        return "sms-" + texts.size();
    };

    @ParameterizedTest
    @ValueSource(strings = {"on", "ON", " on "})
    void onReturnsTheSmsReceiptListener(String setting) {
        assertInstanceOf(SmsReceiptListener.class, ReceiptListenerFactory.create(setting, fakeSms).orElseThrow());
    }

    @Test
    void notSetMeansOn() {
        assertTrue(ReceiptListenerFactory.create(null, fakeSms).isPresent());
    }

    @ParameterizedTest
    @ValueSource(strings = {"off", "OFF", " Off"})
    void offReturnsNoListener(String setting) {
        assertTrue(ReceiptListenerFactory.create(setting, fakeSms).isEmpty());
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "  ", "maybe", "false", "0"})
    void anythingElseIsRejected(String setting) {
        assertThrows(IllegalArgumentException.class, () -> ReceiptListenerFactory.create(setting, fakeSms));
    }

    @Test
    void theReceiptShowsAmountFeeAndWhatTheRecipientReceives() {
        var listener = ReceiptListenerFactory.create("on", fakeSms).orElseThrow();
        var transfer = new Transfer("T0005", "international", "sender-0005", new BigDecimal("200.00"), "MXN");
        listener.onPriced(new Quote(transfer, new BigDecimal("4.00"), new BigDecimal("3450.00"), "MXN"));

        assertEquals(List.of("sender-0005: Transfer T0005: amount USD 200.00, fee USD 4.00, recipient receives MXN 3450.00"),
                texts);
        assertEquals(1, listener.sentCount());
    }
}
