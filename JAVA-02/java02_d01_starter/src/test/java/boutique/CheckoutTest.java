package boutique;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class CheckoutTest {
    @TempDir Path directory;

    @Test
    void reservesAvailableUnits() throws Exception {
        var jacket = new Product("JACKET", 4999, 60);
        var writer = mock(ManifestWriter.class);
        try (var checkout = new Checkout(List.of(jacket), writer)) {
            assertThat(checkout.checkout("order-1", "JACKET", 2)).isTrue();
        }
        assertThat(jacket.stock()).isEqualTo(58);
        verify(writer).append(any(Order.class));
    }

    @Test
    void refusesMoreUnitsThanRemain() throws Exception {
        var jacket = new Product("JACKET", 4999, 2);
        try (var checkout = new Checkout(List.of(jacket), mock(ManifestWriter.class))) {
            assertThat(checkout.checkout("order-1", "JACKET", 3)).isFalse();
        }
        assertThat(jacket.stock()).isEqualTo(2);
    }

    @Test
    void reservesTheLastUnit() throws Exception {
        var jacket = new Product("JACKET", 4999, 1);
        try (var checkout = new Checkout(List.of(jacket), mock(ManifestWriter.class))) {
            assertThat(checkout.checkout("order-1", "JACKET", 1)).isTrue();
        }
        assertThat(jacket.stock()).isZero();
    }

    @Test
    void changesThePrice() throws Exception {
        var jacket = new Product("JACKET", 4999, 60);
        try (var checkout = new Checkout(List.of(jacket), mock(ManifestWriter.class))) {
            checkout.changePrice("JACKET", 4499);
        }
        assertThat(jacket.priceCents()).isEqualTo(4499);
    }

    @Test
    void lowStockUsesTheExistingBoundaryAndProductOrder() throws Exception {
        var soldOut = new Product("EMPTY", 100, 0);
        var low = new Product("LOW", 100, 4);
        var boundary = new Product("FIVE", 100, 5);
        try (var checkout = new Checkout(List.of(soldOut, low, boundary), mock(ManifestWriter.class))) {
            assertThat(checkout.lowStock()).containsExactly(soldOut, low);
        }
    }

    @Test
    void writesOneCompleteOrderLine() throws Exception {
        Path path = directory.resolve("manifest.csv");
        try (var writer = new FileManifestWriter(path)) {
            writer.append(new Order("order-1", "JACKET", 2, 4999));
        }
        assertThat(Files.readAllLines(path)).containsExactly("order-1,JACKET,2,4999");
    }
}
