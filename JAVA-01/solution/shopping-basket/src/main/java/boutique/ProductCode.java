package boutique;

import java.util.Locale;
import java.util.Objects;

/**
 * A product code compared the way staff mean it: ignoring case and surrounding spaces,
 * so {@code "ab-12"} and {@code "AB-12 "} are the same code.
 *
 * <p>A plain class (not a record) on purpose: equality uses the normalized value only,
 * and equals and hashCode are overridden on that same field.
 */
public final class ProductCode {
    private final String normalized;

    private ProductCode(String normalized) {
        this.normalized = normalized;
    }

    /** Throws IllegalArgumentException for a null or blank code (a caller error). */
    public static ProductCode of(String code) {
        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException("Product code must not be blank");
        }
        return new ProductCode(code.trim().toUpperCase(Locale.ROOT));
    }

    public String value() {
        return normalized;
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof ProductCode that && normalized.equals(that.normalized);
    }

    @Override
    public int hashCode() {
        return Objects.hash(normalized);
    }

    @Override
    public String toString() {
        return normalized;
    }
}
