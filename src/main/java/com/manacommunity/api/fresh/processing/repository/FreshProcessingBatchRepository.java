package com.manacommunity.api.fresh.processing.repository;

import com.manacommunity.api.fresh.processing.model.BatchStatus;
import com.manacommunity.api.fresh.processing.model.FreshProcessingBatch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface FreshProcessingBatchRepository extends JpaRepository<FreshProcessingBatch, Long> {
    Optional<FreshProcessingBatch> findByBatchNumber(String batchNumber);
    List<FreshProcessingBatch> findByVendorIdOrderByProductionDateDesc(Long vendorId);
    List<FreshProcessingBatch> findByVendorIdAndStatus(Long vendorId, BatchStatus status);

    @Query("SELECT b FROM FreshProcessingBatch b WHERE b.productionDate = :date ORDER BY b.createdAt DESC")
    List<FreshProcessingBatch> findByProductionDate(LocalDate date);
}