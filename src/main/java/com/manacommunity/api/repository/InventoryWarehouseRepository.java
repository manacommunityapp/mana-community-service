package com.manacommunity.api.repository;

import com.manacommunity.api.model.InventoryWarehouse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface InventoryWarehouseRepository extends JpaRepository<InventoryWarehouse, Long> {

    List<InventoryWarehouse> findByIsActiveTrueOrderByNameAsc();

    List<InventoryWarehouse> findByCommunityIdAndIsActiveTrueOrderByNameAsc(Long communityId);

    Optional<InventoryWarehouse> findByCode(String code);

    boolean existsByCode(String code);
}
