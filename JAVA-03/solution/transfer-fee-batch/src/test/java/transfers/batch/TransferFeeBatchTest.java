package transfers.batch;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import transfers.core.FeeCalculator;
import transfers.core.RateSource;
import transfers.core.UnknownCurrencyException;
import transfers.core.rules.BankTransferFee;
import transfers.core.rules.BillPaymentFee;
import transfers.core.rules.FriendTransferFee;
import transfers.core.rules.InternationalTransferFee;
import transfers.events.PricingEvents;
import transfers.receipts.ReceiptListenerFactory;
import transfers.receipts.SmsService;
import transfers.review.ReviewFlagListener;

class TransferFeeBatchTest {
    @TempDir Path directory;

    private static final RateSource RATES = currency -> switch (currency) {
        case "MXN" -> new BigDecimal("17.25");
        default -> throw new UnknownCurrencyException(currency);
    };

    private final AtomicInteger texts = new AtomicInteger();
    private final SmsService countingSms = (recipient, body) -> "sms-" + texts.incrementAndGet();

    private Path input() throws Exception {
        return Files.writeString(directory.resolve("transfers.csv"), """
                id,type,sender,amount_usd,recipient_currency
                T1,friend,s1,25.00,USD
                T2,intl,s2,40.00,EUR
                T3,bill,s3,120.00,USD
                T4,international,s4,200.00,MXN
                T5,international,s5,50.00,JPY
                T6,bank,s6,6000.00,USD
                """);
    }

    private TransferFeeBatch batch(PricingEvents events) {
        var calculator = new FeeCalculator(List.of(new FriendTransferFee(), new BankTransferFee(),
                new BillPaymentFee(), new InternationalTransferFee()), RATES);
        return new TransferFeeBatch(calculator, events, 4);
    }

    @Test
    void refusedRowsDoNotStopTheBatch() throws Exception {
        Path output = directory.resolve("out/fees.csv");
        var result = batch(new PricingEvents()).run(input(), output);

        assertEquals(new BatchResult(6, 4, 2), result);
        assertEquals(List.of(
                "id,type,amount_usd,fee_usd,recipient_amount,recipient_currency",
                "T1,friend,25.00,0.00,25.00,USD",
                "T3,bill,120.00,0.25,120.00,USD",
                "T4,international,200.00,4.00,3450.00,MXN",
                "T6,bank,6000.00,0.50,6000.00,USD"), Files.readAllLines(output));
    }

    @Test
    void receiptsOnTextsEveryPricedTransfer() throws Exception {
        var events = new PricingEvents();
        ReceiptListenerFactory.create("on", countingSms).ifPresent(events::subscribe);
        batch(events).run(input(), directory.resolve("fees.csv"));
        assertEquals(4, texts.get());
    }

    @Test
    void receiptsOffSendsNoTextsButStillPricesAndFlags() throws Exception {
        var events = new PricingEvents();
        var reviews = new ReviewFlagListener();
        events.subscribe(reviews);
        ReceiptListenerFactory.create("off", countingSms).ifPresent(events::subscribe);

        Path output = directory.resolve("fees.csv");
        var result = batch(events).run(input(), output);

        assertEquals(0, texts.get());
        assertEquals(4, result.priced());
        assertEquals(List.of("T6"), reviews.flaggedIds());
        assertTrue(Files.readString(output).contains("T4,international,200.00,4.00,3450.00,MXN"));
    }
}
