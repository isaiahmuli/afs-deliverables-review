package transfers.receipts;

import java.util.concurrent.atomic.AtomicInteger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Local SMS simulation: waits 5 ms and returns a message ID. Never contacts a real service. */
public final class SimulatedSmsService implements SmsService {
    private static final Logger log = LoggerFactory.getLogger(SimulatedSmsService.class);
    private final AtomicInteger sent = new AtomicInteger();

    @Override
    public String send(String recipient, String body) throws InterruptedException {
        Thread.sleep(5);
        String id = "sms-" + sent.incrementAndGet();
        log.debug("SMS {} to {}: {}", id, recipient, body);
        return id;
    }

    public int sentCount() {
        return sent.get();
    }
}
