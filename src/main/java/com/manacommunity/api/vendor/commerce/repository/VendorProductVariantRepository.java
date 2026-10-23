package com.manacommunity.api.vendor.commerce.repository;

import com.manacommunity.api.vendor.commerce.model.VendorProductVariant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface VendorProductVariantRepository extends JpaRepository<VendorProductVariant, Long> {
    Optional<VendorProductVariant> findBySku(String sku);
}
