package transfers.events;

import transfers.core.Quote;

/** Observer: told about every successfully priced transfer. */
public interface PricedTransferListener {
    void onPriced(Quote quote);
}
