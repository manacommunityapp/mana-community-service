package com.manacommunity.api.fresh.inventory.repository;

import com.manacommunity.api.fresh.inventory.model.FreshStockMovement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FreshStockMovementRepository extends JpaRepository<FreshStockMovement, Long> {

    List<FreshStockMovement> findByVendorProductIdOrderByCreatedAtDesc(Long vendorProductId);

    List<FreshStockMovement> findByBatchIdOrderByCreatedAtDesc(Long batchId);

    Optional<FreshStockMovement> findFirstByVendorProductIdOrderByCreatedAtDesc(Long vendorProductId);
}
