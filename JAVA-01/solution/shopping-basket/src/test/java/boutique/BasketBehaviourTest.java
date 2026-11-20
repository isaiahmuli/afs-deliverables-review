package boutique;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** Tests for the new behaviour. The supplied regression tests are in ShoppingBasketTest. */
class BasketBehaviourTest {

    private static ShoppingBasket basket() {
        return new ShoppingBasket(List.of(
                new Product("AB-12", "Shirt", 2500),
                new Product("cap-7", "Cap", 1800),
                new Product("SOCK-3", "Socks", 799)));
    }

    @Nested
    class Lines {
        @Test
        void sameProductTwiceIsOneLineWithQuantityTwo() throws Exception {
            var basket = basket();
            basket.add("AB-12", 1);
            basket.add("AB-12", 1);
            assertEquals(1, basket.lines().size());
            assertEquals(2, basket.lines().getFirst().quantity());
            assertEquals(5000, basket.lines().getFirst().totalCents());
        }

        @Test
        void codesDifferingInCaseAndSpacesAreOneLine() throws Exception {
            var basket = basket();
            basket.add("ab-12", 1);
            basket.add("AB-12 ", 1);
            var lines = basket.lines();
            assertEquals(1, lines.size());
            assertEquals(2, lines.getFirst().quantity());
            assertEquals("AB-12", lines.getFirst().productCode(), "line shows the catalog's code");
        }

        @Test
        void linesStayInFirstAddedOrder() throws Exception {
            var basket = basket();
            basket.add("cap-7", 1);
            basket.add("AB-12", 1);
            basket.add("CAP-7", 2);
            basket.add("SOCK-3", 1);
            assertEquals(List.of("cap-7", "AB-12", "SOCK-3"),
                    basket.lines().stream().map(BasketLine::productCode).toList());
            assertEquals(3, basket.lines().getFirst().quantity());
        }

        @Test
        void callersCannotAddRemoveOrReplaceLines() throws Exception {
            var basket = basket();
            basket.add("AB-12", 1);
            var lines = basket.lines();
            var extra = new BasketLine("cap-7", "Cap", 1800, 1);
            assertAll(
                    () -> assertThrows(UnsupportedOperationException.class, () -> lines.add(extra)),
                    () -> assertThrows(UnsupportedOperationException.class, () -> lines.remove(0)),
                    () -> assertThrows(UnsupportedOperationException.class, () -> lines.set(0, extra)),
                    () -> assertThrows(UnsupportedOperationException.class, lines::clear));
            assertEquals(2500, basket.subtotalCents());
        }

        @Test
        void callersCannotChangeALinesQuantity() throws Exception {
            // BasketLine is a record: final fields, no setters. A caller's copy never changes,
            // and the basket's own line changes only through add().
            assertTrue(BasketLine.class.isRecord());
            assertTrue(Arrays.stream(BasketLine.class.getDeclaredFields())
                    .allMatch(f -> Modifier.isPrivate(f.getModifiers()) && Modifier.isFinal(f.getModifiers())));
            var basket = basket();
            basket.add("AB-12", 1);
            var before = basket.lines().getFirst();
            basket.add("AB-12", 1);
            assertEquals(1, before.quantity());
            assertEquals(2, basket.lines().getFirst().quantity());
        }

        @Test
        void basketStateIsPrivate() {
            for (Class<?> type : List.of(ShoppingBasket.class, ProductCode.class, Product.class, BasketLine.class)) {
                assertTrue(Arrays.stream(type.getDeclaredFields()).allMatch(f -> Modifier.isPrivate(f.getModifiers())),
                        type.getSimpleName() + " has a non-private field");
            }
        }
    }

    @Nested
    class RejectedInput {
        @Test
        void unknownCodeIsACheckedRecoverableError() throws Exception {
            var basket = basket();
            basket.add("AB-12", 1);
            var error = assertThrows(UnknownProductException.class, () -> basket.add("ZZ-99", 1));
            assertEquals("ZZ-99", error.productCode());
            assertEquals(1, basket.lines().size(), "the basket is unchanged and still usable");
            basket.add("cap-7", 1);
            assertEquals(4300, basket.subtotalCents());
        }

        @ParameterizedTest
        @ValueSource(ints = {0, -1, Integer.MIN_VALUE})
        void nonPositiveQuantityIsRejectedImmediately(int quantity) {
            var basket = basket();
            assertThrows(IllegalArgumentException.class, () -> basket.add("AB-12", quantity));
            assertTrue(basket.lines().isEmpty());
        }

        @Test
        void nonPositiveQuantityIsRejectedBeforeTheCodeIsLookedUp() {
            assertThrows(IllegalArgumentException.class, () -> basket().add("ZZ-99", 0));
        }

        @Test
        void negativePriceIsRejected() {
            assertThrows(IllegalArgumentException.class, () -> new Product("X-1", "Broken", -1));
            assertThrows(IllegalArgumentException.class, () -> new BasketLine("X-1", "Broken", -1, 1));
        }

        @Test
        void zeroPriceIsAllowed() {
            assertEquals(0, new Product("FREE-1", "Gift bag", 0).priceCents());
        }

        @Test
        void blankOrMissingCodeIsRejected() {
            assertThrows(IllegalArgumentException.class, () -> new Product(" ", "Shirt", 2500));
            assertThrows(IllegalArgumentException.class, () -> basket().add("  ", 1));
            assertThrows(IllegalArgumentException.class, () -> basket().add(null, 1));
        }

        @Test
        void duplicateCatalogCodesAreRejected() {
            assertThrows(IllegalArgumentException.class, () -> new ShoppingBasket(List.of(
                    new Product("AB-12", "Shirt", 2500), new Product("ab-12 ", "Other shirt", 900))));
        }

        @Test
        void invalidDiscountsAreRejected() {
            assertAll(
                    () -> assertThrows(IllegalArgumentException.class, () -> new Discount.Percentage(-1)),
                    () -> assertThrows(IllegalArgumentException.class, () -> new Discount.Percentage(101)),
                    () -> assertThrows(IllegalArgumentException.class, () -> new Discount.FixedAmount(-1)),
                    () -> assertThrows(IllegalArgumentException.class, () -> new SpendThresholdDiscount(0, 100)),
                    () -> assertThrows(IllegalArgumentException.class, () -> new SpendThresholdDiscount(5000, 0)),
                    () -> assertThrows(IllegalArgumentException.class, () -> new SpendThresholdDiscount(5000, 5001)));
        }

        @Test
        void missingDeliveryOrDiscountIsRejected() {
            assertThrows(NullPointerException.class, () -> basket().setDelivery(null));
            assertThrows(NullPointerException.class, () -> basket().setDiscount(null));
        }
    }

    @Nested
    class ProductCodes {
        @Test
        void equalCodesActAsOneSetElementAndOneMapKey() {
            var lower = ProductCode.of("ab-12");
            var padded = ProductCode.of("AB-12 ");
            assertEquals(lower, padded);
            assertEquals(lower.hashCode(), padded.hashCode());

            Set<ProductCode> set = new HashSet<>(List.of(lower, padded));
            assertEquals(1, set.size());

            Map<ProductCode, Integer> map = new HashMap<>();
            map.put(lower, 1);
            map.put(padded, 2);
            assertEquals(1, map.size());
            assertEquals(2, map.get(ProductCode.of(" ab-12")));
        }

        @Test
        void differentCodesAreNotEqual() {
            assertNotEquals(ProductCode.of("AB-12"), ProductCode.of("AB-13"));
            assertNotEquals(ProductCode.of("AB-12"), "AB-12");
        }
    }

    @Nested
    class DeliveryOptions {
        @Test
        void pickupIsFree() throws Exception {
            var basket = basket();
            basket.add("AB-12", 1);
            basket.setDelivery(Delivery.PICKUP);
            assertEquals(0, basket.deliveryCents());
            assertEquals(2500, basket.checkoutTotalCents());
        }

        @Test
        void pickupWithDiscountChargesOnlyTheDiscountedItems() throws Exception {
            var basket = basket();
            basket.add("AB-12", 2);
            basket.add("cap-7", 1);
            basket.setDiscount(new Discount.Percentage(10));
            basket.setDelivery(Delivery.PICKUP);
            assertEquals(6120, basket.checkoutTotalCents());
        }

        @Test
        void standardIsTheDefault() throws Exception {
            var basket = basket();
            basket.add("AB-12", 1);
            assertEquals(500, basket.deliveryCents());
        }
    }

    @Nested
    class Discounts {
        @Test
        void everyDiscountIsAppliedThroughTheSupertype() {
            List<Discount> discounts = List.of(
                    new Discount.Percentage(10), new Discount.FixedAmount(700), new SpendThresholdDiscount(5000, 1000));
            assertEquals(List.of(6120L, 6100L, 5800L), discounts.stream().map(d -> d.apply(6800)).toList());
        }

        @Test
        void spendThresholdTakesTheAmountOffAtOrAboveTheThreshold() throws Exception {
            var basket = basket();
            basket.add("AB-12", 2);
            basket.add("cap-7", 1);
            basket.setDiscount(new SpendThresholdDiscount(5000, 1000));
            assertEquals(6800, basket.subtotalCents());
            assertEquals(5800, basket.discountedSubtotalCents());
            assertEquals(6300, basket.checkoutTotalCents());
        }

        @Test
        void spendThresholdCountsTheExactThreshold() throws Exception {
            var basket = basket();
            basket.add("AB-12", 2); // exactly $50.00
            basket.setDiscount(new SpendThresholdDiscount(5000, 1000));
            assertEquals(4000, basket.discountedSubtotalCents());
        }

        @Test
        void spendThresholdGivesNothingBelowTheThreshold() {
            assertEquals(4999, new SpendThresholdDiscount(5000, 1000).apply(4999));
            assertEquals(0, new SpendThresholdDiscount(5000, 1000).apply(0));
        }
    }
}
