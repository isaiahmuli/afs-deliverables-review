package com.example.stock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.stock.OfficeFixture.Office;
import com.example.stock.domain.InsufficientStockException;
import com.example.stock.domain.Product;
import com.example.stock.repository.ProductRepository;
import com.example.stock.repository.StockAdjustmentRepository;
import com.example.stock.service.CatalogService;
import com.example.stock.service.ConcurrentStockChangeException;
import com.example.stock.service.StockService;
import com.example.stock.service.Views.AdjustmentView;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/** D1-4: two staff changing the same supply at once. */
@IntegrationTest
class OptimisticLockingTest {

    @Autowired
    StockService stock;

    @Autowired
    CatalogService catalog;

    @Autowired
    ProductRepository products;

    @Autowired
    StockAdjustmentRepository adjustments;

    @Autowired
    PlatformTransactionManager txManager;

    @Autowired
    JdbcTemplate jdbc;

    Office office;

    @BeforeEach
    void office() {
        OfficeFixture.emptyDatabase(jdbc);
        office = OfficeFixture.seed(catalog, stock);
    }

    @Test
    void twoStaffAtOnceTheSecondIsRejectedAndTheFirstIsKept() throws Exception {
        TransactionTemplate tx = new TransactionTemplate(txManager);
        CyclicBarrier bothHaveReadTen = new CyclicBarrier(2);
        CountDownLatch amaraCommitted = new CountDownLatch(1);

        try (ExecutorService staff = Executors.newFixedThreadPool(2)) {
            // Amara receives 20 pens.
            Future<?> amara = staff.submit(() -> tx.executeWithoutResult(s -> {
                Product pens = products.findById(office.pens()).orElseThrow();
                await(bothHaveReadTen);
                adjustments.save(pens.receive(20, "delivery from supplier", Instant.now()));
                products.saveAndFlush(pens);
            }));
            // Brian takes 5 pens, working from the same count of 10, and saves after Amara.
            Future<?> brian = staff.submit(() -> {
                try {
                    tx.executeWithoutResult(s -> {
                        Product pens = products.findById(office.pens()).orElseThrow();
                        await(bothHaveReadTen);
                        assertThat(pens.getQuantity()).isEqualTo(10);
                        await(amaraCommitted);
                        try {
                            adjustments.save(pens.use(5, "training session", Instant.now()));
                        } catch (InsufficientStockException e) {
                            throw new IllegalStateException(e);
                        }
                        products.saveAndFlush(pens);
                    });
                    return null;
                } catch (OptimisticLockingFailureException e) {
                    return e;
                }
            });
            amara.get(10, TimeUnit.SECONDS);
            amaraCommitted.countDown();

            assertThat(brian.get(10, TimeUnit.SECONDS)).isInstanceOf(OptimisticLockingFailureException.class);
        }

        // Amara's change is kept; Brian's was rejected, not silently applied to a stale count.
        assertThat(stock.quantity(office.pens())).isEqualTo(30);
        assertThat(stock.history(office.pens())).extracting(AdjustmentView::quantityChange).containsExactly(10, 20);

        // Brian retries against the new count, as the error tells him to.
        stock.use(office.pens(), 5, "training session");
        assertThat(stock.quantity(office.pens())).isEqualTo(25);
        assertThat(stock.history(office.pens())).extracting(AdjustmentView::quantityChange).containsExactly(10, 20, -5);
    }

    @Test
    void staleUpdateIsRejectedAndTheSuccessfulUpdateIsPreserved() throws Exception {
        Product staleCopy = products.findById(office.pens()).orElseThrow(); // version 1 (after opening count)
        long staleVersion = staleCopy.getVersion();

        stock.receive(office.pens(), 20, "delivery from supplier");        // pens 30, version + 1

        staleCopy.use(5, "training session", Instant.now());              // still thinks pens = 10
        assertThatThrownBy(() -> products.saveAndFlush(staleCopy))
                .isInstanceOf(OptimisticLockingFailureException.class);

        Product current = products.findById(office.pens()).orElseThrow();
        assertThat(current.getQuantity()).isEqualTo(30);
        assertThat(current.getVersion()).isEqualTo(staleVersion + 1);
    }

    @Test
    void manyStaffUsingTheSameSupplyAtOnceNeverLoseAChange() throws Exception {
        int staffCount = 12;
        CyclicBarrier start = new CyclicBarrier(staffCount);
        List<Future<String>> outcomes = new ArrayList<>();

        try (ExecutorService staff = Executors.newFixedThreadPool(staffCount)) {
            for (int i = 0; i < staffCount; i++) {
                outcomes.add(staff.submit(() -> {
                    await(start);
                    try {
                        stock.use(office.notebooks(), 1, "meeting");
                        return "saved";
                    } catch (ConcurrentStockChangeException e) {
                        return "rejected";
                    }
                }));
            }
        }
        long saved = 0;
        long rejected = 0;
        for (Future<String> f : outcomes) {
            if (f.get().equals("saved")) {
                saved++;
            } else {
                rejected++;
            }
        }
        System.out.printf("[concurrency] %d staff took 1 notebook at once: %d saved, %d rejected with "
                + "ConcurrentStockChangeException, notebooks now %d%n",
                staffCount, saved, rejected, stock.quantity(office.notebooks()));

        assertThat(saved + rejected).isEqualTo(staffCount);
        assertThat(saved).isPositive();
        // Every saved change is in the count and in the history; every other one was reported.
        assertThat(stock.quantity(office.notebooks())).isEqualTo(25 - saved);
        assertThat(stock.history(office.notebooks())).hasSize(1 + (int) saved);
    }

    private static void await(CyclicBarrier barrier) {
        try {
            barrier.await(10, TimeUnit.SECONDS);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private static void await(CountDownLatch latch) {
        try {
            assertThat(latch.await(10, TimeUnit.SECONDS)).isTrue();
        } catch (InterruptedException e) {
            throw new IllegalStateException(e);
        }
    }
}
