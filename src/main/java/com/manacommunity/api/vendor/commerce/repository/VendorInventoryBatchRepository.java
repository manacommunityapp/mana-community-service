package com.manacommunity.api.vendor.commerce.repository;

import com.manacommunity.api.vendor.commerce.model.VendorInventoryBatch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface VendorInventoryBatchRepository extends JpaRepository<VendorInventoryBatch, Long> {
    List<VendorInventoryBatch> findByVariantId(Long variantId);
    List<VendorInventoryBatch> findByExpiryDateBeforeAndRemainingQtyGreaterThan(LocalDate date, Integer minRemaining);
}
