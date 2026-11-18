package transfers.events;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import transfers.core.Quote;

/**
 * Subject: notifies every subscribed listener of each priced transfer. Safe to publish
 * from several batch threads at once. One failing listener does not stop the others.
 */
public final class PricingEvents {
    private static final Logger log = LoggerFactory.getLogger(PricingEvents.class);
    private final List<PricedTransferListener> listeners = new CopyOnWriteArrayList<>();

    public void subscribe(PricedTransferListener listener) {
        listeners.add(listener);
    }

    public void unsubscribe(PricedTransferListener listener) {
        listeners.remove(listener);
    }

    public void publish(Quote quote) {
        for (PricedTransferListener listener : listeners) {
            try {
                listener.onPriced(quote);
            } catch (RuntimeException e) {
                log.error("Listener {} failed for transfer {}", listener.getClass().getSimpleName(),
                        quote.transfer().id(), e);
            }
        }
    }
}
