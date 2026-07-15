package com.manacommunity.api.repository;

import com.manacommunity.api.model.InventoryLocation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InventoryLocationRepository extends JpaRepository<InventoryLocation, Long> {

    List<InventoryLocation> findByWarehouseIdAndIsActiveTrueOrderByCompleteNameAsc(Long warehouseId);

    List<InventoryLocation> findByIsActiveTrueOrderByCompleteNameAsc();

    List<InventoryLocation> findByUsageAndIsActiveTrueOrderByCompleteNameAsc(InventoryLocation.LocationUsage usage);
}
