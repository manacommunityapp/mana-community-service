package com.manacommunity.api.fresh.catalog.repository;

import com.manacommunity.api.fresh.catalog.model.FreshProductCatalogProfile;
import com.manacommunity.api.fresh.vendor.model.FreshVendorCapabilityType;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface FreshProductCatalogProfileRepository extends JpaRepository<FreshProductCatalogProfile, Long> {
    Optional<FreshProductCatalogProfile> findByVendorProductId(Long vendorProductId);
    List<FreshProductCatalogProfile> findByCapabilityType(FreshVendorCapabilityType capabilityType);
}