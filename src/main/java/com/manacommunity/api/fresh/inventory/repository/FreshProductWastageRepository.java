package com.manacommunity.api.fresh.inventory.repository;

import com.manacommunity.api.fresh.inventory.model.FreshProductWastage;
import com.manacommunity.api.fresh.inventory.model.WastageCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FreshProductWastageRepository extends JpaRepository<FreshProductWastage, Long> {

    List<FreshProductWastage> findByVendorProductId(Long vendorProductId);

    List<FreshProductWastage> findByBatchId(Long batchId);

    List<FreshProductWastage> findByWastageCategory(WastageCategory category);

    Optional<FreshProductWastage> findByWastageReference(String wastageReference);
}
