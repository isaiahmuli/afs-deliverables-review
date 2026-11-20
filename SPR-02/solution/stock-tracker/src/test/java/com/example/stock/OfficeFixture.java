package com.example.stock;

import com.example.stock.service.CatalogService;
import com.example.stock.service.StockService;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;

/** The brief's example office, with pens at 10 so the brief's +20 / -5 example can be replayed. */
public final class OfficeFixture {

    public record Office(long pens, long staples, long notebooks, long tape) {
    }

    private OfficeFixture() {
    }

    public static void emptyDatabase(JdbcTemplate jdbc) {
        jdbc.execute("TRUNCATE stock_adjustment, product_category, product, category RESTART IDENTITY CASCADE");
    }

    public static Office seed(CatalogService catalog, StockService stock) {
        catalog.addCategory("Stationery");
        catalog.addCategory("Meeting Supplies");
        catalog.addCategory("Maintenance");
        Office office = new Office(
                catalog.addProduct("Ballpoint pens"),
                catalog.addProduct("Staples"),
                catalog.addProduct("Notebooks"),
                catalog.addProduct("Painter's tape"));
        catalog.assignCategories(office.pens(), List.of("Stationery"));
        catalog.assignCategories(office.staples(), List.of("Stationery"));
        catalog.assignCategories(office.notebooks(), List.of("Stationery", "Meeting Supplies"));
        catalog.assignCategories(office.tape(), List.of("Maintenance"));
        stock.receive(office.pens(), 10, "opening count");
        stock.receive(office.staples(), 12, "opening count");
        stock.receive(office.notebooks(), 25, "opening count");
        stock.receive(office.tape(), 8, "opening count");
        return office;
    }
}
