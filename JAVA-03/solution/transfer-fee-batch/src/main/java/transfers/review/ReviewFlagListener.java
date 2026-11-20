package transfers.review;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import transfers.core.Quote;
import transfers.events.PricedTransferListener;

/** Flags transfers above $5,000 for review (exactly $5,000 is not flagged). */
public final class ReviewFlagListener implements PricedTransferListener {
    private static final Logger log = LoggerFactory.getLogger(ReviewFlagListener.class);
    private static final BigDecimal LIMIT = new BigDecimal("5000");
    private final List<String> flagged = new CopyOnWriteArrayList<>();

    @Override
    public void onPriced(Quote quote) {
        BigDecimal amount = quote.transfer().amountUsd();
        if (amount.compareTo(LIMIT) > 0) {
            flagged.add(quote.transfer().id());
            log.warn("REVIEW {} USD {}", quote.transfer().id(), amount.toPlainString());
        }
    }

    public List<String> flaggedIds() {
        return List.copyOf(flagged);
    }
}
