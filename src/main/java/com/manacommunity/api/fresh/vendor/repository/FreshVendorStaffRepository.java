package com.manacommunity.api.fresh.vendor.repository;

import com.manacommunity.api.fresh.vendor.model.FreshVendorStaff;
import com.manacommunity.api.fresh.vendor.model.VendorStaffRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FreshVendorStaffRepository extends JpaRepository<FreshVendorStaff, Long> {

    List<FreshVendorStaff> findByVendorId(Long vendorId);

    List<FreshVendorStaff> findByVendorIdAndIsActiveTrue(Long vendorId);

    Optional<FreshVendorStaff> findByVendorIdAndUserId(Long vendorId, Long userId);

    List<FreshVendorStaff> findByUserIdAndIsActiveTrue(Long userId);

    boolean existsByVendorIdAndUserIdAndIsActiveTrue(Long vendorId, Long userId);

    boolean existsByVendorIdAndUserIdAndVendorRoleAndIsActiveTrue(Long vendorId, Long userId, VendorStaffRole vendorRole);
}
