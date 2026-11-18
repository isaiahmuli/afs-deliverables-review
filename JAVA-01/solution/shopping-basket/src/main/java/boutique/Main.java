package boutique;

import java.math.BigDecimal;
import java.util.List;

public class Main {
    public static void main(String[] args) throws Exception {
        var basket = new ShoppingBasket(List.of(
                new Product("AB-12", "Shirt", 2500),
                new Product("cap-7", "Cap", 1800),
                new Product("SOCK-3", "Socks", 799)));
        basket.add("AB-12", 1);
        basket.add("ab-12", 1);
        basket.add("cap-7", 1);
        basket.setDiscount(new Discount.Percentage(10));
        basket.setDelivery(Delivery.STANDARD);

        System.out.println("BOUTIQUE RECEIPT");
        for (BasketLine line : basket.lines()) {
            System.out.printf("%s | %s | quantity %d | $%s%n",
                    line.productCode(), line.productName(), line.quantity(), money(line.totalCents()));
        }
        System.out.println("Items: $" + money(basket.subtotalCents()));
        System.out.println("After discount: $" + money(basket.discountedSubtotalCents()));
        System.out.println("Delivery: $" + money(basket.deliveryCents()));
        System.out.println("Total: $" + money(basket.checkoutTotalCents()));
    }

    private static String money(long cents) {
        return BigDecimal.valueOf(cents, 2).toPlainString();
    }
}
