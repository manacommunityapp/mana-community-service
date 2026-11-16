package com.manacommunity.api.fresh.catalog.repository;

import com.manacommunity.api.fresh.catalog.model.FreshProductAllergen;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface FreshProductAllergenRepository extends JpaRepository<FreshProductAllergen, Long> {
    List<FreshProductAllergen> findByVendorProductId(Long vendorProductId);
    void deleteByVendorProductIdAndAllergenName(Long vendorProductId, String allergenName);
}