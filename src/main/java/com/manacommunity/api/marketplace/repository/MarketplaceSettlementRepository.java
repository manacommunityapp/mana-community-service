package com.manacommunity.api.marketplace.repository;

import com.manacommunity.api.marketplace.entity.MarketplaceSettlementRecord;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.time.LocalDateTime;

@Repository
public interface MarketplaceSettlementRepository extends JpaRepository<MarketplaceSettlementRecord, Long> {

    List<MarketplaceSettlementRecord> findBySellerId(Long sellerId);

    Page<MarketplaceSettlementRecord> findBySellerId(Long sellerId, Pageable pageable);

    Optional<MarketplaceSettlementRecord> findByOrderNumber(String orderNumber);

    Optional<MarketplaceSettlementRecord> findBySettlementReference(String settlementReference);

    List<MarketplaceSettlementRecord> findByStatus(MarketplaceSettlementRecord.SettlementStatus status);
    List<MarketplaceSettlementRecord> findByCreatedAtBetween(LocalDateTime from, LocalDateTime to);

    @Query("SELECT COUNT(s) FROM MarketplaceSettlementRecord s WHERE s.settledAt IS NULL " +
            "AND s.createdAt < :cutoff AND s.status IN (" +
            "com.manacommunity.api.marketplace.entity.MarketplaceSettlementRecord.SettlementStatus.PENDING_CLEARANCE, " +
            "com.manacommunity.api.marketplace.entity.MarketplaceSettlementRecord.SettlementStatus.SCHEDULED)")
    long countOverduePlatformSettlements(@Param("cutoff") LocalDateTime cutoff);
}
