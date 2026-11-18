package boutique;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.util.List;
import java.util.Random;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;

/** Rules added on top of the supplied CheckoutTest. */
class CheckoutRulesTest {

    @SuppressWarnings("unchecked")
    private static RecordWriter<AuditEvent> auditMock() {
        return mock(RecordWriter.class);
    }

    @ParameterizedTest(name = "stock 5, quantity {0} -> reserved={1}, stock left {2}")
    @CsvSource({
            "-1, false, 5",   // invalid: negative
            "0,  false, 5",   // invalid: zero
            "1,  true,  4",   // smallest valid
            "4,  true,  1",
            "5,  true,  0",   // boundary: exactly the stock
            "6,  false, 5",   // boundary: one more than the stock
            "2147483647, false, 5"
    })
    void quantityBoundaries(int quantity, boolean reserved, int stockLeft) throws Exception {
        var jacket = new Product("JACKET", 4999, 5);
        var writer = mock(ManifestWriter.class);
        try (var checkout = new Checkout(List.of(jacket), writer)) {
            assertThat(checkout.checkout("order-1", "JACKET", quantity)).isEqualTo(reserved);
        }
        assertThat(jacket.stock()).isEqualTo(stockLeft);
        if (reserved) {
            verify(writer).append(any(Order.class));
        } else {
            verify(writer, never()).append(any());
        }
    }

    @Test
    void refusedOrdersNeverReachTheManifestOrTheAuditLog() throws Exception {
        var jacket = new Product("JACKET", 4999, 1);
        var manifest = mock(ManifestWriter.class);
        var audit = auditMock();
        try (var checkout = new Checkout(List.of(jacket), manifest, audit, 5)) {
            assertThat(checkout.checkout("order-1", "JACKET", 2)).isFalse();
            assertThat(checkout.checkout("order-2", "JACKET", 0)).isFalse();
            assertThat(checkout.checkout("order-3", "NOPE", 1)).isFalse();
        }
        verify(manifest, never()).append(any());
        verify(audit, never()).append(any());
        verify(manifest).close();
        verify(audit).close();
    }

    @Test
    void aReservationGoesToTheManifestAndTheAuditLog() throws Exception {
        var jacket = new Product("JACKET", 4999, 60);
        var manifest = mock(ManifestWriter.class);
        var audit = auditMock();
        try (var checkout = new Checkout(List.of(jacket), manifest, audit, 5)) {
            assertThat(checkout.checkout("order-7", "JACKET", 2)).isTrue();
        }
        var order = ArgumentCaptor.forClass(Order.class);
        verify(manifest).append(order.capture());
        assertThat(order.getValue().toCsvLine()).isEqualTo("order-7,JACKET,2,4999");
        var event = ArgumentCaptor.forClass(AuditEvent.class);
        verify(audit).append(event.capture());
        assertThat(event.getValue()).isInstanceOfSatisfying(AuditEvent.Reservation.class, reservation -> {
            assertThat(reservation.orderId()).isEqualTo("order-7");
            assertThat(reservation.quantity()).isEqualTo(2);
            assertThat(reservation.unitPriceCents()).isEqualTo(4999);
        });
    }

    @Test
    void aPriceChangeIsAudited() throws Exception {
        var jacket = new Product("JACKET", 4999, 60);
        var manifest = mock(ManifestWriter.class);
        var audit = auditMock();
        try (var checkout = new Checkout(List.of(jacket), manifest, audit, 5)) {
            assertThat(checkout.changePrice("JACKET", 4499)).isTrue();
            assertThat(checkout.changePrice("NOPE", 100)).isFalse();
        }
        var event = ArgumentCaptor.forClass(AuditEvent.class);
        verify(audit).append(event.capture());
        assertThat(event.getValue()).isInstanceOfSatisfying(AuditEvent.PriceChange.class, change -> {
            assertThat(change.productCode()).isEqualTo("JACKET");
            assertThat(change.oldPriceCents()).isEqualTo(4999);
            assertThat(change.newPriceCents()).isEqualTo(4499);
        });
        assertThat(event.getValue().toCsvLine()).endsWith(",PRICE_CHANGED,JACKET,4999,4499");
        verify(manifest, never()).append(any());
    }

    @Test
    void aNegativePriceIsRejected() throws Exception {
        var jacket = new Product("JACKET", 4999, 60);
        var audit = auditMock();
        try (var checkout = new Checkout(List.of(jacket), mock(ManifestWriter.class), audit, 5)) {
            assertThatThrownBy(() -> checkout.changePrice("JACKET", -1)).isInstanceOf(IllegalArgumentException.class);
        }
        assertThat(jacket.priceCents()).isEqualTo(4999);
        verify(audit, never()).append(any());
    }

    @Test
    void findProductReturnsAnOptional() throws Exception {
        var jacket = new Product("JACKET", 4999, 60);
        try (var checkout = new Checkout(List.of(jacket), mock(ManifestWriter.class))) {
            assertThat(checkout.findProduct("JACKET")).containsSame(jacket);
            assertThat(checkout.findProduct("jacket")).isEmpty();
            assertThat(checkout.findProduct("NOPE")).isEmpty();
            assertThat(checkout.findProduct(null)).isEmpty();
        }
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"NOPE", "jacket", " JACKET"})
    void anUnknownCodeIsRefusedWithoutAnException(String code) throws Exception {
        var writer = mock(ManifestWriter.class);
        try (var checkout = new Checkout(List.of(new Product("JACKET", 4999, 60)), writer)) {
            assertThatCode(() -> assertThat(checkout.checkout("order-1", code, 1)).isFalse())
                    .doesNotThrowAnyException();
        }
        verify(writer, never()).append(any());
    }

    @Test
    void theStoreChoosesTheLowStockThreshold() throws Exception {
        var empty = new Product("EMPTY", 100, 0);
        var three = new Product("THREE", 100, 3);
        var nine = new Product("NINE", 100, 9);
        var ten = new Product("TEN", 100, 10);
        var products = List.of(ten, empty, nine, three);
        try (var at0 = new Checkout(products, mock(ManifestWriter.class), auditMock(), 0);
             var at4 = new Checkout(products, mock(ManifestWriter.class), auditMock(), 4);
             var at10 = new Checkout(products, mock(ManifestWriter.class), auditMock(), 10)) {
            assertThat(at0.lowStock()).isEmpty();
            assertThat(at4.lowStock()).containsExactly(empty, three);
            assertThat(at10.lowStock()).containsExactly(empty, nine, three);
        }
    }

    @Test
    void aNegativeThresholdIsRejected() {
        assertThatThrownBy(() -> new Checkout(List.of(), mock(ManifestWriter.class), auditMock(), -1))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void theStreamMatchesTheOriginalLoopAtThresholdFive() throws Exception {
        var random = new Random(42);
        for (int round = 0; round < 50; round++) {
            List<Product> products = IntStream.range(0, 30)
                    .mapToObj(i -> new Product("P" + i, 100, random.nextInt(12)))
                    .toList();
            try (var checkout = new Checkout(products, mock(ManifestWriter.class))) {
                assertThat(checkout.lowStock())
                        .isNotEmpty()
                        .containsExactlyElementsOf(checkout.originalLowStockLoop());
            }
        }
    }

    @Test
    void productsMatchingTakesALambdaOrAMethodReference() throws Exception {
        var empty = new Product("EMPTY", 100, 0);
        var full = new Product("FULL", 100, 50);
        try (var checkout = new Checkout(List.of(full, empty), mock(ManifestWriter.class))) {
            assertThat(checkout.productsMatching(product -> product.stock() > 10)).containsExactly(full);
            assertThat(checkout.productsMatching(Product::isSoldOut)).containsExactly(empty);
        }
    }

    @Test
    void invalidProductsAreRejected() {
        assertThatThrownBy(() -> new Product(" ", 100, 1)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Product("X", -1, 1)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Product("X", 100, -1)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void closeClosesTheAuditLogEvenIfTheManifestFails() throws Exception {
        var manifest = mock(ManifestWriter.class);
        var audit = auditMock();
        org.mockito.Mockito.doThrow(new java.io.IOException("disk full")).when(manifest).close();
        var checkout = new Checkout(List.of(), manifest, audit, 5);
        assertThatThrownBy(checkout::close).hasMessage("disk full");
        verify(audit).close();
    }
}
