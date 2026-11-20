package transfers.receipts;

import java.util.Locale;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Factory for the receipt-off control. Support sets {@code RECEIPTS=on} or {@code RECEIPTS=off}
 * (missing means on). Returns the SMS receipt listener when on, and nothing when off, so no
 * text is sent while pricing and review flags carry on.
 */
public final class ReceiptListenerFactory {
    private static final Logger log = LoggerFactory.getLogger(ReceiptListenerFactory.class);

    private ReceiptListenerFactory() {}

    /** @throws IllegalArgumentException for anything other than on, off, or null (not set) */
    public static Optional<SmsReceiptListener> create(String setting, SmsService sms) {
        String mode = setting == null ? "on" : setting.trim().toLowerCase(Locale.ROOT);
        return switch (mode) {
            case "on" -> Optional.of(new SmsReceiptListener(sms));
            case "off" -> {
                log.warn("Receipts are OFF: no texts will be sent");
                yield Optional.empty();
            }
            default -> throw new IllegalArgumentException("RECEIPTS must be on or off, not: " + setting);
        };
    }
}
