package com.manacommunity.api.vendor.repository;

import com.manacommunity.api.vendor.entity.VmsVendorOwner;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface VmsVendorOwnerRepository extends JpaRepository<VmsVendorOwner, Long> {
    List<VmsVendorOwner> findByVendorId(Long vendorId);
}
