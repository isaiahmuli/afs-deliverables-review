package boutique;

public class Product {
    public String code;
    public String name;
    public long priceCents;

    public Product(String code, String name, long priceCents) {
        this.code = code;
        this.name = name;
        this.priceCents = priceCents;
    }

    public String code() { return code; }
    public String name() { return name; }
    public long priceCents() { return priceCents; }
}
