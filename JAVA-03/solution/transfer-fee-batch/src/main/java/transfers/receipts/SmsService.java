package transfers.receipts;

/** Sends a text message and returns its message ID. */
public interface SmsService {
    String send(String recipient, String body) throws InterruptedException;
}
