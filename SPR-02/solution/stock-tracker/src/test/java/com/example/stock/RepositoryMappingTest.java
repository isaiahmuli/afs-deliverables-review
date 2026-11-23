package com.example.stock;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.stock.domain.Category;
import com.example.stock.domain.InsufficientStockException;
import com.example.stock.domain.Product;
import com.example.stock.domain.StockAdjustment;
import com.example.stock.repository.ProductRepository;
import com.example.stock.repository.StockAdjustmentRepository;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;

/**
 * JPA slice on PostgreSQL 18. Each test flushes and clears the persistence context before
 * reading, so values really come back from the database, not from Hibernate's cache.
 */
@DataJpaTest
@Import(TestcontainersConfiguration.class)
@TestPropertySource(locations = "classpath:test.properties")
class RepositoryMappingTest {

    @Autowired
    TestEntityManager em;

    @Autowired
    ProductRepository products;

    @Autowired
    StockAdjustmentRepository adjustments;

    private Category stationery;
    private Category meetings;

    @BeforeEach
    void catalog() {
        stationery = em.persist(new Category("Stationery"));
        meetings = em.persist(new Category("Meeting Supplies"));
        Category maintenance = em.persist(new Category("Maintenance"));
        Product pens = new Product("Ballpoint pens");
        pens.replaceCategories(List.of(stationery));
        Product staples = new Product("Staples");
        staples.replaceCategories(List.of(stationery));
        Product notebooks = new Product("Notebooks");
        notebooks.replaceCategories(List.of(stationery, meetings));
        Product tape = new Product("Painter's tape");
        tape.replaceCategories(List.of(maintenance));
        Product paper = new Product("100% recycled paper");
        paper.replaceCategories(List.of(stationery));
        List.of(pens, staples, notebooks, tape, paper).forEach(em::persist);
        em.flush();
        em.clear();
    }

    private static List<String> names(List<Product> list) {
        return list.stream().map(Product::getName).toList();
    }

    @Test
    void runsOnPostgres18() {
        Object version = em.getEntityManager().createNativeQuery("show server_version").getSingleResult();
        assertThat(version.toString()).startsWith("18.");
    }

    @Test
    void manyToManyPersistsAndReloadsFromBothSides() {
        Product notebooks = products.findByNameIgnoreCase("notebooks").orElseThrow();
        assertThat(notebooks.getCategories()).extracting(Category::getName)
                .containsExactlyInAnyOrder("Stationery", "Meeting Supplies");

        Category reloaded = em.find(Category.class, stationery.getId());
        assertThat(reloaded.getProducts()).extracting(Product::getName)
                .containsExactlyInAnyOrder("Ballpoint pens", "Staples", "Notebooks", "100% recycled paper");
        assertThat(em.find(Category.class, meetings.getId()).getProducts()).extracting(Product::getName)
                .containsExactly("Notebooks");
    }

    @Test
    void oneToManyHistoryPersistsAndReloadsInOrder() throws InsufficientStockException {
        Product pens = products.findByNameIgnoreCase("Ballpoint pens").orElseThrow();
        Instant t = Instant.parse("2026-11-20T09:00:00Z");
        StockAdjustment opening = pens.receive(10, "opening count", t);
        StockAdjustment delivery = pens.receive(20, "delivery from supplier", t.plusSeconds(60));
        StockAdjustment used = pens.use(5, "training session", t.plusSeconds(120));
        adjustments.saveAll(List.of(opening, delivery, used));
        em.flush();
        em.clear();

        Product reloaded = em.find(Product.class, pens.getId());
        assertThat(reloaded.getQuantity()).isEqualTo(25);
        assertThat(reloaded.getAdjustments())
                .extracting(StockAdjustment::getQuantityChange, StockAdjustment::getQuantityAfter, StockAdjustment::getReason)
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple(10, 10, "opening count"),
                        org.assertj.core.groups.Tuple.tuple(20, 30, "delivery from supplier"),
                        org.assertj.core.groups.Tuple.tuple(-5, 25, "training session"));
        assertThat(adjustments.findByProductIdOrderByAdjustedAtAscIdAsc(pens.getId()))
                .allSatisfy(a -> assertThat(a.getProduct().getId()).isEqualTo(pens.getId()));
    }

    @Test
    void derivedSearchMatchesPartOfTheNameIgnoringCase() {
        assertThat(names(products.findByNameContainingIgnoreCaseOrderByNameAsc("pen"))).containsExactly("Ballpoint pens");
        assertThat(names(products.findByNameContainingIgnoreCaseOrderByNameAsc("PAINTER'S"))).containsExactly("Painter's tape");
        assertThat(names(products.findByNameContainingIgnoreCaseOrderByNameAsc("s"))).hasSize(4);
        assertThat(products.findByNameContainingIgnoreCaseOrderByNameAsc("xyz")).isEmpty();
    }

    @Test
    void queryMethodFindsProductsInACategory() {
        assertThat(names(products.findInCategory("stationery")))
                .containsExactly("100% recycled paper", "Ballpoint pens", "Notebooks", "Staples");
        assertThat(names(products.findInCategory("Meeting Supplies"))).containsExactly("Notebooks");
    }

    @ParameterizedTest(name = "search for [{0}] returns nothing")
    @ValueSource(strings = {"' OR '1'='1", "'; DROP TABLE product; --", "_", "pens' --"})
    void injectionPayloadsInSearchAreMatchedLiterally(String payload) {
        assertThat(products.findByNameContainingIgnoreCaseOrderByNameAsc(payload)).isEmpty();
        assertThat(products.count()).isEqualTo(5);
    }

    @Test
    void wildcardCharactersAreMatchedLiterally() {
        // "%" would match every product if it reached LIKE unescaped; it only matches the name containing it.
        assertThat(names(products.findByNameContainingIgnoreCaseOrderByNameAsc("%"))).containsExactly("100% recycled paper");
        assertThat(names(products.findByNameContainingIgnoreCaseOrderByNameAsc("'"))).containsExactly("Painter's tape");
    }

    @Test
    void injectionPayloadInCategoryQueryIsABindParameter() {
        assertThat(products.findInCategory("Stationery' OR '1'='1")).isEmpty();
        assertThat(products.findInCategory("x') OR 1=1 --")).isEmpty();
    }
}
