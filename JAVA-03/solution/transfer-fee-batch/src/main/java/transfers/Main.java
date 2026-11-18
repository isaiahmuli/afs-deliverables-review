package transfers;

import java.nio.file.Path;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import transfers.batch.BatchResult;
import transfers.batch.TransferFeeBatch;
import transfers.core.FeeCalculator;
import transfers.core.rules.BankTransferFee;
import transfers.core.rules.BillPaymentFee;
import transfers.core.rules.FriendTransferFee;
import transfers.core.rules.InternationalTransferFee;
import transfers.events.PricingEvents;
import transfers.rates.BankRates;
import transfers.receipts.ReceiptListenerFactory;
import transfers.receipts.SimulatedSmsService;
import transfers.receipts.SmsReceiptListener;
import transfers.review.ReviewFlagListener;

/**
 * Wires the night's batch. Receipts are on unless {@code RECEIPTS=off} is set (environment
 * variable, or {@code -Dreceipts=off}).
 */
public final class Main {
    private static final Logger log = LoggerFactory.getLogger(Main.class);

    private Main() {}

    public static void main(String[] args) throws Exception {
        if (args.length != 0 && args.length != 3) {
            throw new IllegalArgumentException("Supply no arguments, or: transfers.csv bank-rates.csv output.csv");
        }
        Path input = Path.of(args.length == 0 ? "transfers.csv" : args[0]);
        Path rates = Path.of(args.length == 0 ? "bank-rates.csv" : args[1]);
        Path output = Path.of(args.length == 0 ? "build/output/fees.csv" : args[2]);

        var calculator = new FeeCalculator(List.of(
                new FriendTransferFee(), new BankTransferFee(), new BillPaymentFee(), new InternationalTransferFee()),
                BankRates.getInstance(rates));

        var events = new PricingEvents();
        var reviews = new ReviewFlagListener();
        events.subscribe(reviews);
        String receiptSetting = System.getProperty("receipts", System.getenv("RECEIPTS"));
        var receipts = ReceiptListenerFactory.create(receiptSetting, new SimulatedSmsService());
        receipts.ifPresent(events::subscribe);

        BatchResult result = new TransferFeeBatch(calculator, events, 4).run(input, output);

        log.info("Read={} priced={} refused={} rate fetches={} SMS receipts={} review flags={}",
                result.read(), result.priced(), result.refused(), BankRates.fetchCount(),
                receipts.map(SmsReceiptListener::sentCount).orElse(0), reviews.flaggedIds().size());
    }
}
