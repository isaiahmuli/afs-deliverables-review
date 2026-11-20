package boutique;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

public class ShoppingBasket {
    public List<Product> catalog;
    public List<BasketLine> items = new ArrayList<>();
    public Discount discount;
    public String delivery = "STANDARD";

    public ShoppingBasket(List<Product> catalog) {
        this.catalog = new ArrayList<>(catalog);
    }

    public void add(String productCode, int quantity) {
        for (Product product : catalog) {
            if (product.code().trim().equalsIgnoreCase(productCode.trim())) {
                items.add(new BasketLine(productCode, product.name(), product.priceCents(), quantity));
                return;
            }
        }
        throw new IllegalArgumentException("Unknown product: " + productCode);
    }

    public void setDiscount(Discount discount) {
        this.discount = discount;
    }

    public void setDelivery(String delivery) {
        this.delivery = delivery;
    }

    public List<BasketLine> lines() {
        return items;
    }

    public long subtotalCents() {
        long subtotal = 0;
        for (BasketLine item : items) {
            subtotal = Math.addExact(subtotal, item.totalCents());
        }
        return subtotal;
    }

    public long discountedSubtotalCents() {
        long subtotal = subtotalCents();
        if (discount instanceof Discount.Percentage) {
            int percent = ((Discount.Percentage) discount).percent;
            long discountCents = BigDecimal.valueOf(subtotal)
                    .multiply(BigDecimal.valueOf(percent))
                    .divide(BigDecimal.valueOf(100), 0, RoundingMode.HALF_UP)
                    .longValueExact();
            return Math.max(0, subtotal - discountCents);
        } else if (discount instanceof Discount.FixedAmount) {
            long amount = ((Discount.FixedAmount) discount).cents;
            return Math.max(0, subtotal - amount);
        }
        return subtotal;
    }

    public long deliveryCents() {
        if ("STANDARD".equals(delivery)) {
            return 500;
        } else if ("EXPRESS".equals(delivery)) {
            return 1200;
        }
        return 0;
    }

    public long checkoutTotalCents() {
        return Math.addExact(discountedSubtotalCents(), deliveryCents());
    }
}
