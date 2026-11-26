package com.example.stock.service;

import com.example.stock.domain.Category;
import com.example.stock.repository.ProductRepository;
import com.example.stock.service.Views.StockLine;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class StockReportService {

    private final ProductRepository products;

    public StockReportService(ProductRepository products) {
        this.products = products;
    }

    /** Every product with its quantity and categories, by name. One SQL query at any catalog size. */
    @Transactional(readOnly = true)
    public List<StockLine> report() {
        return products.findAllWithCategories().stream()
                .map(p -> new StockLine(p.getName(), p.getQuantity(),
                        p.getCategories().stream().map(Category::getName).sorted().toList()))
                .toList();
    }
}
