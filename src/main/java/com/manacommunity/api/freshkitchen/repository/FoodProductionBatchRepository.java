package com.manacommunity.api.freshkitchen.repository;

import com.manacommunity.api.freshkitchen.model.BatchStatus;
import com.manacommunity.api.freshkitchen.model.FoodProductionBatch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface FoodProductionBatchRepository extends JpaRepository<FoodProductionBatch, Long> {

    Optional<FoodProductionBatch> findByBatchNumber(String batchNumber);

    List<FoodProductionBatch> findByVendorId(String vendorId);

    List<FoodProductionBatch> findByVendorIdAndProductionDate(String vendorId, LocalDate productionDate);

    List<FoodProductionBatch> findByProductId(String productId);

    List<FoodProductionBatch> findByBatchStatus(BatchStatus batchStatus);

    List<FoodProductionBatch> findByProductionDate(LocalDate productionDate);

    List<FoodProductionBatch> findByBatchStatusAndExpiryDateTimeBefore(BatchStatus status, LocalDateTime expiryThreshold);
}
