package boutique;

/**
 * A catalog product. {@code code} is the code as the catalog displays it (for example
 * {@code cap-7}); lookups use {@link #key()}, which ignores case and spaces.
 */
public record Product(String code, String name, long priceCents) {
    public Product {
        ProductCode.of(code); // rejects a null or blank code
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Product name must not be blank");
        }
        if (priceCents < 0) {
            throw new IllegalArgumentException("Price must not be negative: " + priceCents);
        }
    }

    public ProductCode key() {
        return ProductCode.of(code);
    }
}
