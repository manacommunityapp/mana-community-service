package com.manacommunity.api.fresh.catalog.repository;

import com.manacommunity.api.fresh.catalog.model.FreshProductNutrition;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface FreshProductNutritionRepository extends JpaRepository<FreshProductNutrition, Long> {
    Optional<FreshProductNutrition> findByVendorProductId(Long vendorProductId);
}