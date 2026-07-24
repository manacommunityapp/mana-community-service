package com.manacommunity.api.vendor.repository;

import com.manacommunity.api.vendor.entity.VmsSettlement;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VmsSettlementRepository extends JpaRepository<VmsSettlement, Long> {
    Page<VmsSettlement> findByVendorId(Long vendorId, Pageable pageable);
    Page<VmsSettlement> findByCommunityIdAndStatus(Long communityId, String status, Pageable pageable);
}
