package boutique;

import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

class ShoppingBasketTest {
    private ShoppingBasket basket() {
        return new ShoppingBasket(List.of(
                new Product("AB-12", "Shirt", 2500),
                new Product("cap-7", "Cap", 1800),
                new Product("SOCK-3", "Socks", 799),
                new Product("CENT-5", "Small sample", 5)));
    }

    @Test
    void addsPricesAndQuantities() throws Exception {
        var basket = basket();
        basket.add("AB-12", 2);
        basket.add("cap-7", 1);
        assertEquals(6800, basket.subtotalCents());
        assertEquals(7300, basket.checkoutTotalCents());
    }

    @Test
    void standardDeliveryCostsFiveDollars() throws Exception {
        var basket = basket();
        basket.add("AB-12", 1);
        basket.setDelivery(Delivery.STANDARD);
        assertEquals(500, basket.deliveryCents());
        assertEquals(3000, basket.checkoutTotalCents());
    }

    @Test
    void expressDeliveryCostsTwelveDollars() throws Exception {
        var basket = basket();
        basket.add("AB-12", 1);
        basket.setDelivery(Delivery.EXPRESS);
        assertEquals(1200, basket.deliveryCents());
        assertEquals(3700, basket.checkoutTotalCents());
    }

    @Test
    void percentageDiscountAppliesBeforeDelivery() throws Exception {
        var basket = basket();
        basket.add("AB-12", 2);
        basket.add("cap-7", 1);
        basket.setDiscount(new Discount.Percentage(10));
        assertEquals(6120, basket.discountedSubtotalCents());
        assertEquals(6620, basket.checkoutTotalCents());
    }

    @Test
    void percentageDiscountRoundsHalfUp() throws Exception {
        var basket = basket();
        basket.add("CENT-5", 1);
        basket.setDiscount(new Discount.Percentage(10));
        assertEquals(4, basket.discountedSubtotalCents());
        assertEquals(504, basket.checkoutTotalCents());
    }

    @Test
    void fixedDiscountAppliesBeforeDelivery() throws Exception {
        var basket = basket();
        basket.add("AB-12", 2);
        basket.setDiscount(new Discount.FixedAmount(700));
        assertEquals(4300, basket.discountedSubtotalCents());
        assertEquals(4800, basket.checkoutTotalCents());
    }

    @Test
    void fixedDiscountCannotMakeMerchandiseNegative() throws Exception {
        var basket = basket();
        basket.add("AB-12", 1);
        basket.setDiscount(new Discount.FixedAmount(4000));
        assertEquals(0, basket.discountedSubtotalCents());
        assertEquals(500, basket.checkoutTotalCents());
    }

    @Test
    void repeatedAddsPreserveTheTotal() throws Exception {
        var basket = basket();
        basket.add("AB-12", 1);
        basket.add("ab-12", 1);
        assertEquals(5000, basket.subtotalCents());
        assertEquals(5500, basket.checkoutTotalCents());
    }

    @Test
    void lineDescribesTheAddedProduct() throws Exception {
        var basket = basket();
        basket.add("AB-12", 2);
        var line = basket.lines().getFirst();
        assertEquals("AB-12", line.productCode());
        assertEquals("Shirt", line.productName());
        assertEquals(2500, line.unitPriceCents());
        assertEquals(2, line.quantity());
        assertEquals(5000, line.totalCents());
    }
}
