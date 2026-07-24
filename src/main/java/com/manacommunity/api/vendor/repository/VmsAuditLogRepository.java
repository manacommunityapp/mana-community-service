package com.manacommunity.api.vendor.repository;

import com.manacommunity.api.vendor.entity.VmsAuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VmsAuditLogRepository extends JpaRepository<VmsAuditLog, Long> {
    Page<VmsAuditLog> findByEntityTypeAndEntityId(String entityType, Long entityId, Pageable pageable);
    Page<VmsAuditLog> findByCommunityId(Long communityId, Pageable pageable);
}
