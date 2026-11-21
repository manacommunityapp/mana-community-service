package com.manacommunity.api.vendor.repository;

import com.manacommunity.api.vendor.entity.VmsSettlement;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface VmsSettlementRepository extends JpaRepository<VmsSettlement, Long> {
    Page<VmsSettlement> findByVendorId(Long vendorId, Pageable pageable);
    Page<VmsSettlement> findByCommunityIdAndStatus(Long communityId, String status, Pageable pageable);
    List<VmsSettlement> findByPeriodEndBetween(LocalDate from, LocalDate to);

    @Query("SELECT COUNT(s) FROM VmsSettlement s WHERE s.status = 'PENDING' AND s.paidAt IS NULL AND s.periodEnd < :cutoff")
    long countOverduePlatformSettlements(@Param("cutoff") LocalDate cutoff);
}
