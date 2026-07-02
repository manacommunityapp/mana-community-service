package com.manacommunity.api.inventory.repository;

import com.manacommunity.api.inventory.entity.StockWarehouse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface StockWarehouseRepository extends JpaRepository<StockWarehouse, Long> {
    Optional<StockWarehouse> findByCode(String code);
}
