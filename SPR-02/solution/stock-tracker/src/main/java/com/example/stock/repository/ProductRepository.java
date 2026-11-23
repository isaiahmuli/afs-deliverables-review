package com.example.stock.repository;

import com.example.stock.domain.Product;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProductRepository extends JpaRepository<Product, Long> {

    /** Derived query. The input is bound, and Spring Data escapes % and _ for "Containing". */
    List<Product> findByNameContainingIgnoreCaseOrderByNameAsc(String part);

    Optional<Product> findByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCase(String name);

    /** Products in one category; the category name is a bind parameter. */
    @Query("""
            select p from Product p join p.categories c
            where lower(c.name) = lower(:category)
            order by p.name""")
    List<Product> findInCategory(@Param("category") String category);

    /** The stock report: products and their categories in one query (no N+1). */
    @Query("select distinct p from Product p left join fetch p.categories order by p.name")
    List<Product> findAllWithCategories();
}
