package com.example.stock.repository;

import com.example.stock.domain.StockAdjustment;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StockAdjustmentRepository extends JpaRepository<StockAdjustment, Long> {

    List<StockAdjustment> findByProductIdOrderByAdjustedAtAscIdAsc(Long productId);

    long countByProductId(Long productId);
}
