package transfers.events;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import transfers.core.Quote;
import transfers.core.Transfer;

class PricingEventsTest {
    private static Quote quote(String id) {
        var transfer = new Transfer(id, "bank", "s", new BigDecimal("10.00"), "USD");
        return new Quote(transfer, new BigDecimal("0.50"), new BigDecimal("10.00"), "USD");
    }

    @Test
    void subscribedObserversAreNotifiedAndUnsubscribedOnesAreNot() {
        var events = new PricingEvents();
        List<String> first = new ArrayList<>();
        List<String> second = new ArrayList<>();
        PricedTransferListener a = q -> first.add(q.transfer().id());
        PricedTransferListener b = q -> second.add(q.transfer().id());

        events.subscribe(a);
        events.subscribe(b);
        events.publish(quote("T1"));
        events.unsubscribe(b);
        events.publish(quote("T2"));

        assertEquals(List.of("T1", "T2"), first);
        assertEquals(List.of("T1"), second);
    }

    @Test
    void aFailingObserverDoesNotStopTheOthers() {
        var events = new PricingEvents();
        List<String> received = new ArrayList<>();
        events.subscribe(q -> { throw new IllegalStateException("SMS down"); });
        events.subscribe(q -> received.add(q.transfer().id()));
        events.publish(quote("T1"));
        assertEquals(List.of("T1"), received);
    }
}
