package com.example.stock;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.stock.domain.Category;
import com.example.stock.repository.ProductRepository;
import com.example.stock.service.CatalogService;
import com.example.stock.service.StockReportService;
import com.example.stock.service.StockService;
import com.example.stock.service.Views.StockLine;
import jakarta.persistence.EntityManagerFactory;
import java.util.ArrayList;
import java.util.List;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/** D1-5: SQL statements counted by Hibernate statistics, at several catalog sizes. */
@IntegrationTest
class StockReportQueryCountTest {

    @Autowired
    StockReportService reports;

    @Autowired
    ProductRepository products;

    @Autowired
    CatalogService catalog;

    @Autowired
    StockService stock;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    PlatformTransactionManager txManager;

    @Autowired
    EntityManagerFactory emf;

    Statistics stats;

    @BeforeEach
    void clean() {
        OfficeFixture.emptyDatabase(jdbc);
        stats = emf.unwrap(SessionFactory.class).getStatistics();
    }

    /** n products, each in 2 of 3 categories, inserted with plain JDBC. */
    private void catalogOf(int n) {
        jdbc.update("insert into category (name) values ('Stationery'), ('Meeting Supplies'), ('Maintenance')");
        List<Object[]> rows = new ArrayList<>();
        for (int i = 1; i <= n; i++) {
            rows.add(new Object[] {"Product %04d".formatted(i), i % 40});
        }
        jdbc.batchUpdate("insert into product (name, quantity) values (?, ?)", rows);
        jdbc.update("insert into product_category select p.id, 1 + (p.id % 3) from product p");
        jdbc.update("insert into product_category select p.id, 1 + ((p.id + 1) % 3) from product p");
    }

    /** The report written the obvious way: load products, then touch each lazy category set. */
    private List<StockLine> naiveReport() {
        return new TransactionTemplate(txManager).execute(s -> products.findAll(Sort.by("name")).stream()
                .map(p -> new StockLine(p.getName(), p.getQuantity(),
                        p.getCategories().stream().map(Category::getName).sorted().toList()))
                .toList());
    }

    private long statementsFor(Runnable work) {
        stats.clear();
        work.run();
        return stats.getPrepareStatementCount();
    }

    @ParameterizedTest(name = "{0} products")
    @ValueSource(ints = {10, 50, 200})
    void naiveReportIsNPlusOneAndTheFetchJoinIsOneQuery(int size) {
        catalogOf(size);

        List<List<StockLine>> results = new ArrayList<>();
        long before = statementsFor(() -> results.add(naiveReport()));
        long after = statementsFor(() -> results.add(reports.report()));

        System.out.printf("[N+1] %3d products: naive report %3d SQL statements, fixed report %d SQL statement%n",
                size, before, after);

        assertThat(before).as("1 query for products + 1 per product for its categories").isEqualTo(1 + size);
        assertThat(after).as("one join-fetch query").isEqualTo(1);
        assertThat(results.get(1)).hasSize(size).isEqualTo(results.get(0));
        assertThat(results.get(1)).allSatisfy(line -> assertThat(line.categories()).hasSize(2));
    }

    @Test
    void reportShowsQuantitiesAndCategories() throws Exception {
        var office = OfficeFixture.seed(catalog, stock);
        stock.receive(office.pens(), 20, "delivery from supplier");
        stock.use(office.pens(), 5, "training session");

        assertThat(reports.report()).containsExactly(
                new StockLine("Ballpoint pens", 25, List.of("Stationery")),
                new StockLine("Notebooks", 25, List.of("Meeting Supplies", "Stationery")),
                new StockLine("Painter's tape", 8, List.of("Maintenance")),
                new StockLine("Staples", 12, List.of("Stationery")));
    }
}
