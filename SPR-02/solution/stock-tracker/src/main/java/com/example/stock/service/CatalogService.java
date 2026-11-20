package com.example.stock.service;

import com.example.stock.domain.Category;
import com.example.stock.domain.Product;
import com.example.stock.repository.CategoryRepository;
import com.example.stock.repository.ProductRepository;
import com.example.stock.service.Views.ProductView;
import java.util.Collection;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CatalogService {

    private final ProductRepository products;
    private final CategoryRepository categories;

    public CatalogService(ProductRepository products, CategoryRepository categories) {
        this.products = products;
        this.categories = categories;
    }

    @Transactional
    public long addProduct(String name) {
        if (name != null && products.existsByNameIgnoreCase(name.strip())) {
            throw new IllegalArgumentException("a product named '" + name.strip() + "' already exists");
        }
        return products.save(new Product(name)).getId();
    }

    @Transactional
    public long addCategory(String name) {
        if (name != null && categories.existsByNameIgnoreCase(name.strip())) {
            throw new IllegalArgumentException("a category named '" + name.strip() + "' already exists");
        }
        return categories.save(new Category(name)).getId();
    }

    /** Sets the product's categories to exactly these. */
    @Transactional
    public void assignCategories(long productId, Collection<String> categoryNames) {
        Product product = products.findById(productId).orElseThrow(() -> new ProductNotFoundException(productId));
        List<Category> found = categoryNames.stream()
                .map(n -> categories.findByNameIgnoreCase(n).orElseThrow(() -> new CategoryNotFoundException(n)))
                .toList();
        product.replaceCategories(found);
    }

    /** Products whose name contains the text, ignoring case. Blank text matches nothing. */
    @Transactional(readOnly = true)
    public List<ProductView> search(String partOfName) {
        if (partOfName == null || partOfName.isBlank()) {
            return List.of();
        }
        return products.findByNameContainingIgnoreCaseOrderByNameAsc(partOfName.strip()).stream()
                .map(CatalogService::view).toList();
    }

    @Transactional(readOnly = true)
    public List<ProductView> productsInCategory(String categoryName) {
        if (categoryName == null || categories.findByNameIgnoreCase(categoryName).isEmpty()) {
            throw new CategoryNotFoundException(categoryName);
        }
        return products.findInCategory(categoryName).stream().map(CatalogService::view).toList();
    }

    static ProductView view(Product p) {
        return new ProductView(p.getId(), p.getName(), p.getQuantity());
    }
}
