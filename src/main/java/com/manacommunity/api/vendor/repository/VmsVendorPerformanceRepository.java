package com.manacommunity.api.vendor.repository;

import com.manacommunity.api.vendor.entity.VmsVendorPerformance;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface VmsVendorPerformanceRepository extends JpaRepository<VmsVendorPerformance, Long> {
    Optional<VmsVendorPerformance> findByVendorIdAndCommunityId(Long vendorId, Long communityId);
    Page<VmsVendorPerformance> findByCommunityIdOrderByPerformanceScoreDesc(Long communityId, Pageable pageable);
}
