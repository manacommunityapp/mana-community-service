package com.manacommunity.api.inventory.repository;

import com.manacommunity.api.inventory.entity.PickingTypeCode;
import com.manacommunity.api.inventory.entity.StockPickingType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface StockPickingTypeRepository extends JpaRepository<StockPickingType, Long> {
    Optional<StockPickingType> findByWarehouseIdAndCode(Long warehouseId, PickingTypeCode code);
    List<StockPickingType> findByWarehouseId(Long warehouseId);
}
