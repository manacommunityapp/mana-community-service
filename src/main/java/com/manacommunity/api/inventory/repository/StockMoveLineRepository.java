package com.manacommunity.api.inventory.repository;

import com.manacommunity.api.inventory.entity.StockMoveLine;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface StockMoveLineRepository extends JpaRepository<StockMoveLine, Long> {
    List<StockMoveLine> findByPickingId(Long pickingId);
    List<StockMoveLine> findByMoveId(Long moveId);
    List<StockMoveLine> findByProductId(Long productId);
}
