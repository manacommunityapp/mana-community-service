package com.manacommunity.api.marketplace.repository;

import com.manacommunity.api.marketplace.entity.MarketAuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MarketAuditLogRepository extends JpaRepository<MarketAuditLog, Long> {
    Page<MarketAuditLog> findByCommunityIdOrderByCreatedAtDesc(Long communityId, Pageable pageable);
    List<MarketAuditLog> findTop50ByCommunityIdOrderByCreatedAtDesc(Long communityId);
}
