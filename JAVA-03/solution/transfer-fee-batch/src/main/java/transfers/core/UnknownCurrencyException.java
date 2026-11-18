package transfers.core;

/** No rate for this currency. Thrown instead of passing a null rate on. */
public class UnknownCurrencyException extends RuntimeException {
    public UnknownCurrencyException(String currency) {
        super("No bank rate for currency: " + currency);
    }
}
