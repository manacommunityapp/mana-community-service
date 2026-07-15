package com.manacommunity.api.repository;

import com.manacommunity.api.model.InventoryPickingType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface InventoryPickingTypeRepository extends JpaRepository<InventoryPickingType, Long> {
    List<InventoryPickingType> findByWarehouseIdOrderByCodeAsc(Long warehouseId);
    List<InventoryPickingType> findAllByOrderByCodeAsc();
}
