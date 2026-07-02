package com.manacommunity.api.inventory.repository;

import com.manacommunity.api.inventory.entity.StockLot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface StockLotRepository extends JpaRepository<StockLot, Long> {
    Optional<StockLot> findByProductIdAndName(Long productId, String name);
    List<StockLot> findByProductId(Long productId);
}
