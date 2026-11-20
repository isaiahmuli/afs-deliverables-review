package boutique;

/**
 * Checked: an unknown product code is a recoverable failure (for example, staff mistyped
 * the code), so the compiler makes every caller of {@code add} handle it.
 */
public class UnknownProductException extends Exception {
    private final String productCode;

    public UnknownProductException(String productCode) {
        super("Unknown product: " + productCode);
        this.productCode = productCode;
    }

    public String productCode() {
        return productCode;
    }
}
