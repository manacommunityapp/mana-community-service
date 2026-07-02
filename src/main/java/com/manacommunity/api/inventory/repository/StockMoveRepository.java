package com.manacommunity.api.inventory.repository;

import com.manacommunity.api.inventory.entity.StockMove;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface StockMoveRepository extends JpaRepository<StockMove, Long> {
    List<StockMove> findByPickingId(Long pickingId);
}
