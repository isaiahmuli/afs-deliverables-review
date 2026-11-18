package boutique;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ConcurrentLinkedQueue;
import org.junit.jupiter.api.RepeatedTest;
import org.mockito.ArgumentCaptor;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class ConcurrentSaleTest {
    // A race does not show on every run, so the sale is repeated.
    @RepeatedTest(25)
    void onlySixtyShoppersReserveAndOnlyThoseOrdersReachTheManifest() throws Exception {
        var jacket = new Product("JACKET", 4999, 60);
        var writer = mock(ManifestWriter.class);
        var accepted = new ConcurrentLinkedQueue<String>();
        var failures = new ConcurrentLinkedQueue<Throwable>();
        var ready = new CountDownLatch(200);
        var start = new CountDownLatch(1);
        Thread[] shoppers = new Thread[200];

        try (var checkout = new Checkout(List.of(jacket), writer)) {
            for (int i = 0; i < shoppers.length; i++) {
                String id = "order-" + (i + 1);
                shoppers[i] = new Thread(() -> {
                    ready.countDown();
                    try {
                        start.await();
                        if (checkout.checkout(id, "JACKET", 1)) accepted.add(id);
                    } catch (Throwable error) {
                        failures.add(error);
                    }
                });
                shoppers[i].start();
            }
            try {
                assertThat(ready.await(10, java.util.concurrent.TimeUnit.SECONDS)).isTrue();
            } finally {
                start.countDown();
                for (Thread shopper : shoppers) shopper.join();
            }
        }

        assertThat(failures).isEmpty();
        assertThat(accepted).hasSize(60);
        assertThat(jacket.stock()).isZero();
        var orders = ArgumentCaptor.forClass(Order.class);
        verify(writer, times(60)).append(orders.capture());
        assertThat(orders.getAllValues()).extracting(Order::id)
                .containsExactlyInAnyOrderElementsOf(accepted);
    }
}
