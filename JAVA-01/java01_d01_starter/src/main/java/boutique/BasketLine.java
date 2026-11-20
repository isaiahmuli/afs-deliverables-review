package boutique;

public class BasketLine {
    public String productCode;
    public String productName;
    public long unitPriceCents;
    public int quantity;

    public BasketLine(String productCode, String productName, long unitPriceCents, int quantity) {
        this.productCode = productCode;
        this.productName = productName;
        this.unitPriceCents = unitPriceCents;
        this.quantity = quantity;
    }

    public String productCode() { return productCode; }
    public String productName() { return productName; }
    public long unitPriceCents() { return unitPriceCents; }
    public int quantity() { return quantity; }
    public long totalCents() { return Math.multiplyExact(unitPriceCents, quantity); }
}
