package com.example.stock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.stock.OfficeFixture.Office;
import com.example.stock.domain.InsufficientStockException;
import com.example.stock.service.CatalogService;
import com.example.stock.service.StockLedger;
import com.example.stock.service.StockService;
import com.example.stock.service.Views.SlipLine;
import jakarta.persistence.EntityManagerFactory;
import java.util.List;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

/**
 * D1-3. A slip "receive 20 pens, use 50 staples": the pens line is written (and flushed to
 * PostgreSQL) before the staples line fails.
 */
@IntegrationTest
@Import(TransactionRollbackTest.PitfallConfig.class)
class TransactionRollbackTest {

    /**
     * The pitfall, kept only in tests: the same slip logic as StockService.applySlip, but with a
     * plain @Transactional. Spring only rolls back on unchecked exceptions by default, and
     * InsufficientStockException is checked, so the earlier writes are committed.
     */
    static class SlipWithoutRollbackFor {

        private final StockLedger ledger;

        SlipWithoutRollbackFor(StockLedger ledger) {
            this.ledger = ledger;
        }

        @Transactional
        public void applySlip(List<SlipLine> lines) throws InsufficientStockException {
            for (SlipLine line : lines) {
                ledger.apply(line.productId(), line.change(), line.reason());
            }
        }
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class PitfallConfig {
        @Bean
        SlipWithoutRollbackFor slipWithoutRollbackFor(StockLedger ledger) {
            return new SlipWithoutRollbackFor(ledger);
        }
    }

    @Autowired
    StockService stock;

    @Autowired
    CatalogService catalog;

    @Autowired
    SlipWithoutRollbackFor pitfall;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    EntityManagerFactory emf;

    Office office;
    List<SlipLine> slip;

    @BeforeEach
    void office() {
        OfficeFixture.emptyDatabase(jdbc);
        office = OfficeFixture.seed(catalog, stock);
        slip = List.of(
                new SlipLine(office.pens(), 20, "delivery from supplier"),
                new SlipLine(office.staples(), -50, "conference packs"));
    }

    private int pensHistory() {
        return stock.history(office.pens()).size();
    }

    @Test
    void failedSlipRollsBackTheWriteThatAlreadyHappened() {
        Statistics stats = emf.unwrap(SessionFactory.class).getStatistics();
        stats.clear();

        assertThatThrownBy(() -> stock.applySlip(slip))
                .isInstanceOf(InsufficientStockException.class)
                .hasMessage("Cannot remove 50 of Staples: only 12 in stock.");

        long updates = stats.getEntityUpdateCount();
        long inserts = stats.getEntityInsertCount();
        System.out.printf("[rollback] during the failed slip: %d product update and %d history insert were sent "
                + "to PostgreSQL; afterwards pens = %d, pens history rows = %d%n",
                updates, inserts, stock.quantity(office.pens()), pensHistory());

        assertThat(updates).as("pens UPDATE was executed before staples failed").isEqualTo(1);
        assertThat(inserts).as("pens history INSERT was executed before staples failed").isEqualTo(1);
        assertThat(stock.quantity(office.pens())).isEqualTo(10);
        assertThat(stock.quantity(office.staples())).isEqualTo(12);
        assertThat(pensHistory()).isEqualTo(1);
    }

    @Test
    void pitfallBeforeTheFixCheckedExceptionCommitsHalfTheSlip() {
        assertThatThrownBy(() -> pitfall.applySlip(slip)).isInstanceOf(InsufficientStockException.class);

        System.out.printf("[pitfall: plain @Transactional] the slip failed, but pens = %d and pens history rows = %d%n",
                stock.quantity(office.pens()), pensHistory());

        assertThat(stock.quantity(office.pens())).as("the pens line was committed").isEqualTo(30);
        assertThat(pensHistory()).isEqualTo(2);
    }

    @Test
    void pitfallAfterTheFixRollbackForUndoesTheWholeSlip() {
        assertThatThrownBy(() -> stock.applySlip(slip)).isInstanceOf(InsufficientStockException.class);

        System.out.printf("[fixed: rollbackFor = InsufficientStockException] pens = %d, pens history rows = %d%n",
                stock.quantity(office.pens()), pensHistory());

        assertThat(stock.quantity(office.pens())).isEqualTo(10);
        assertThat(pensHistory()).isEqualTo(1);
    }
}
