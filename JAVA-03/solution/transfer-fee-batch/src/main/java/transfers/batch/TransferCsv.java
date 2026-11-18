package transfers.batch;

import java.math.BigDecimal;
import transfers.core.Quote;
import transfers.core.Transfer;

/** The batch's file formats. Plain comma-separated fields, no quoting. */
final class TransferCsv {
    static final String OUTPUT_HEADER = "id,type,amount_usd,fee_usd,recipient_amount,recipient_currency";

    private TransferCsv() {}

    static Transfer parse(String line) {
        String[] columns = line.split(",", -1);
        if (columns.length != 5) {
            throw new IllegalArgumentException("Expected 5 columns: " + line);
        }
        return new Transfer(columns[0], columns[1], columns[2], new BigDecimal(columns[3]), columns[4]);
    }

    static String format(Quote quote) {
        Transfer transfer = quote.transfer();
        return String.join(",", transfer.id(), transfer.type(), transfer.amountUsd().toPlainString(),
                quote.feeUsd().toPlainString(), quote.recipientAmount().toPlainString(), quote.recipientCurrency());
    }
}
