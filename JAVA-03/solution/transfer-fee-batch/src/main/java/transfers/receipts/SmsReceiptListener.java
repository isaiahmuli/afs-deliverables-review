package transfers.receipts;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import transfers.core.Quote;
import transfers.events.PricedTransferListener;

/** Texts the sender a receipt: amount, fee, and what the recipient receives. */
public final class SmsReceiptListener implements PricedTransferListener {
    private static final Logger log = LoggerFactory.getLogger(SmsReceiptListener.class);
    private final SmsService sms;
    private final AtomicInteger sent = new AtomicInteger();

    public SmsReceiptListener(SmsService sms) {
        this.sms = Objects.requireNonNull(sms, "sms");
    }

    static String receiptText(Quote quote) {
        return "Transfer " + quote.transfer().id()
                + ": amount USD " + quote.transfer().amountUsd().toPlainString()
                + ", fee USD " + quote.feeUsd().toPlainString()
                + ", recipient receives " + quote.recipientCurrency() + " " + quote.recipientAmount().toPlainString();
    }

    @Override
    public void onPriced(Quote quote) {
        try {
            String messageId = sms.send(quote.transfer().sender(), receiptText(quote));
            sent.incrementAndGet();
            log.debug("Receipt {} for transfer {}", messageId, quote.transfer().id());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("Receipt for transfer {} not sent: interrupted", quote.transfer().id());
        }
    }

    public int sentCount() {
        return sent.get();
    }
}
