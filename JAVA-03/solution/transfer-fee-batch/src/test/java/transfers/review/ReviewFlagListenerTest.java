package transfers.review;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import transfers.core.Quote;
import transfers.core.Transfer;

class ReviewFlagListenerTest {
    private static Quote quote(String id, String amount) {
        var transfer = new Transfer(id, "friend", "s", new BigDecimal(amount), "USD");
        return new Quote(transfer, BigDecimal.ZERO, new BigDecimal(amount), "USD");
    }

    @Test
    void onlyTransfersAboveFiveThousandAreFlagged() {
        var reviews = new ReviewFlagListener();
        reviews.onPriced(quote("T1", "4999.99"));
        reviews.onPriced(quote("T2", "5000.00"));
        reviews.onPriced(quote("T3", "5000.01"));
        reviews.onPriced(quote("T4", "6000.00"));
        assertEquals(List.of("T3", "T4"), reviews.flaggedIds());
    }
}
