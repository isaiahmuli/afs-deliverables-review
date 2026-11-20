package com.example.stock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

import com.example.stock.OfficeFixture.Office;
import com.example.stock.domain.InsufficientStockException;
import com.example.stock.service.CatalogService;
import com.example.stock.service.CategoryNotFoundException;
import com.example.stock.service.ProductNotFoundException;
import com.example.stock.service.StockService;
import com.example.stock.service.Views.AdjustmentView;
import com.example.stock.service.Views.ProductView;
import com.example.stock.service.Views.SlipLine;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

@IntegrationTest
class StockServiceTest {

    @Autowired
    StockService stock;

    @Autowired
    CatalogService catalog;

    @Autowired
    JdbcTemplate jdbc;

    Office office;

    @BeforeEach
    void office() {
        OfficeFixture.emptyDatabase(jdbc);
        office = OfficeFixture.seed(catalog, stock);
    }

    private int historyRows() {
        return jdbc.queryForObject("select count(*) from stock_adjustment", Integer.class);
    }

    @Test
    void receivingAndUsingUpdateStockAndHistory() throws InsufficientStockException {
        stock.receive(office.pens(), 20, "delivery from supplier");
        assertThat(stock.quantity(office.pens())).isEqualTo(30);

        stock.use(office.pens(), 5, "training session");
        assertThat(stock.quantity(office.pens())).isEqualTo(25);

        assertThat(stock.history(office.pens()))
                .extracting(AdjustmentView::quantityChange, AdjustmentView::quantityAfter, AdjustmentView::reason)
                .containsExactly(
                        tuple(10, 10, "opening count"),
                        tuple(20, 30, "delivery from supplier"),
                        tuple(-5, 25, "training session"));
    }

    @Test
    void unknownProductIsNotFoundAndNothingIsWritten() {
        int before = historyRows();

        assertThatThrownBy(() -> stock.receive(999, 5, "delivery"))
                .isInstanceOf(ProductNotFoundException.class).hasMessage("No product with id 999.");
        assertThatThrownBy(() -> stock.use(999, 5, "meeting")).isInstanceOf(ProductNotFoundException.class);
        assertThatThrownBy(() -> stock.history(999)).isInstanceOf(ProductNotFoundException.class);
        assertThat(historyRows()).isEqualTo(before);
    }

    @Test
    void goingNegativeIsRefusedWithTheReasonAndChangesNothing() {
        assertThatThrownBy(() -> stock.use(office.tape(), 9, "repaint meeting room"))
                .isInstanceOf(InsufficientStockException.class)
                .hasMessage("Cannot remove 9 of Painter's tape: only 8 in stock.");

        assertThat(stock.quantity(office.tape())).isEqualTo(8);
        assertThat(stock.history(office.tape())).hasSize(1);
    }

    @Test
    void usingExactlyWhatIsLeftReachesZero() throws InsufficientStockException {
        stock.use(office.tape(), 8, "repaint meeting room");
        assertThat(stock.quantity(office.tape())).isZero();
    }

    @Test
    void zeroNegativeOrUnexplainedAdjustmentsAreRefused() {
        int before = historyRows();

        assertThatThrownBy(() -> stock.receive(office.pens(), 0, "x")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> stock.receive(office.pens(), -3, "x")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> stock.use(office.pens(), 0, "x")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> stock.receive(office.pens(), 5, "  ")).isInstanceOf(IllegalArgumentException.class)
                .hasMessage("reason must not be blank");
        assertThatThrownBy(() -> stock.applySlip(List.of(new SlipLine(office.pens(), 0, "x"))))
                .isInstanceOf(IllegalArgumentException.class);

        assertThat(historyRows()).isEqualTo(before);
        assertThat(stock.quantity(office.pens())).isEqualTo(10);
    }

    @Test
    void slipAppliesEveryLine() throws InsufficientStockException {
        stock.applySlip(List.of(
                new SlipLine(office.pens(), 20, "delivery"),
                new SlipLine(office.staples(), -2, "meeting")));

        assertThat(stock.quantity(office.pens())).isEqualTo(30);
        assertThat(stock.quantity(office.staples())).isEqualTo(10);
    }

    @Test
    void productsCanBeInSeveralCategoriesAndFoundByCategory() {
        assertThat(catalog.productsInCategory("Stationery")).extracting(ProductView::name)
                .containsExactly("Ballpoint pens", "Notebooks", "Staples");
        assertThat(catalog.productsInCategory("meeting supplies")).extracting(ProductView::name)
                .containsExactly("Notebooks");
        assertThat(catalog.productsInCategory("Maintenance")).extracting(ProductView::name)
                .containsExactly("Painter's tape");
        assertThatThrownBy(() -> catalog.productsInCategory("Kitchen")).isInstanceOf(CategoryNotFoundException.class);
    }

    @Test
    void assigningAnUnknownCategoryChangesNothing() {
        assertThatThrownBy(() -> catalog.assignCategories(office.tape(), List.of("Stationery", "Kitchen")))
                .isInstanceOf(CategoryNotFoundException.class);
        assertThat(catalog.productsInCategory("Stationery")).extracting(ProductView::name)
                .doesNotContain("Painter's tape");
    }

    @Test
    void searchFindsPartOfANameOrNothing() {
        assertThat(catalog.search("pen")).extracting(ProductView::name).containsExactly("Ballpoint pens");
        assertThat(catalog.search("Painter's")).extracting(ProductView::name, ProductView::quantity)
                .containsExactly(tuple("Painter's tape", 8));
        assertThat(catalog.search("xyz")).isEmpty();
        assertThat(catalog.search(" ")).isEmpty();
        assertThat(catalog.search("' OR '1'='1")).isEmpty();
    }

    @Test
    void duplicateProductNamesAreRefused() {
        assertThatThrownBy(() -> catalog.addProduct("staples")).isInstanceOf(IllegalArgumentException.class);
    }
}
