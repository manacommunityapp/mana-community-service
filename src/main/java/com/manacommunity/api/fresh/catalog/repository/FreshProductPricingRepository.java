package com.manacommunity.api.fresh.catalog.repository;

import com.manacommunity.api.fresh.catalog.model.FreshProductPricing;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;
import java.util.Optional;

public interface FreshProductPricingRepository extends JpaRepository<FreshProductPricing, Long> {

    @Query("SELECT p FROM FreshProductPricing p WHERE p.vendorProduct.id = :productId AND p.isActive = true ORDER BY p.effectiveFrom DESC")
    Optional<FreshProductPricing> findActiveByProductId(Long productId);

    List<FreshProductPricing> findByVendorProductIdOrderByEffectiveFromDesc(Long productId);

    @Query("SELECT p FROM FreshProductPricing p WHERE p.adminApproved = false ORDER BY p.createdAt ASC")
    List<FreshProductPricing> findPendingAdminApproval();
}