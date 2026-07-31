package com.manacommunity.api.vendor.repository;

import com.manacommunity.api.vendor.entity.VmsVendorBranch;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface VmsVendorBranchRepository extends JpaRepository<VmsVendorBranch, Long> {
    List<VmsVendorBranch> findByVendorIdAndActiveTrue(Long vendorId);
}
