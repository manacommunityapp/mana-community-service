package com.manacommunity.api.inventory.repository;

import com.manacommunity.api.inventory.entity.StockLocation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface StockLocationRepository extends JpaRepository<StockLocation, Long> {
    Optional<StockLocation> findByCompleteName(String completeName);
    List<StockLocation> findByWarehouseId(Long warehouseId);
}
