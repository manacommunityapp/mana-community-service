package com.manacommunity.api.marketplace.repository;

import com.manacommunity.api.marketplace.entity.MarketplaceSettlementRecord;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MarketplaceSettlementRepository extends JpaRepository<MarketplaceSettlementRecord, Long> {

    List<MarketplaceSettlementRecord> findBySellerId(Long sellerId);

    Page<MarketplaceSettlementRecord> findBySellerId(Long sellerId, Pageable pageable);

    Optional<MarketplaceSettlementRecord> findByOrderNumber(String orderNumber);

    Optional<MarketplaceSettlementRecord> findBySettlementReference(String settlementReference);

    List<MarketplaceSettlementRecord> findByStatus(MarketplaceSettlementRecord.SettlementStatus status);
}
