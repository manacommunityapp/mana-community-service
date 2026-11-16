package com.manacommunity.api.fresh.vendor.repository;

import com.manacommunity.api.fresh.vendor.model.CapabilityStatus;
import com.manacommunity.api.fresh.vendor.model.FreshVendorCapability;
import com.manacommunity.api.fresh.vendor.model.FreshVendorCapabilityType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FreshVendorCapabilityRepository extends JpaRepository<FreshVendorCapability, Long> {

    List<FreshVendorCapability> findByVendorId(Long vendorId);

    Optional<FreshVendorCapability> findByVendorIdAndCapabilityType(Long vendorId, FreshVendorCapabilityType capabilityType);

    List<FreshVendorCapability> findByStatus(CapabilityStatus status);

    List<FreshVendorCapability> findByCapabilityTypeAndStatus(FreshVendorCapabilityType capabilityType, CapabilityStatus status);

    boolean existsByVendorIdAndCapabilityTypeAndStatus(Long vendorId, FreshVendorCapabilityType capabilityType, CapabilityStatus status);
}
