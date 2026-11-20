package boutique;

public class Product {
    private final String code;
    private long priceCents;
    private int stock;

    public Product(String code, long priceCents, int stock) {
        this.code = code;
        this.priceCents = priceCents;
        this.stock = stock;
    }

    public String code() { return code; }
    public long priceCents() { return priceCents; }
    public int stock() { return stock; }
    public void setStock(int stock) { this.stock = stock; }
    public void setPriceCents(long priceCents) { this.priceCents = priceCents; }
}
