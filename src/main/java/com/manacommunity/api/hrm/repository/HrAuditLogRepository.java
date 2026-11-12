package com.manacommunity.api.hrm.repository;

import com.manacommunity.api.hrm.entity.HrAuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface HrAuditLogRepository extends JpaRepository<HrAuditLog, Long> {

    Page<HrAuditLog> findByOrganizationIdOrderByCreatedAtDesc(Long orgId, Pageable pageable);

    List<HrAuditLog> findByOrganizationIdAndEntityTypeAndEntityIdOrderByCreatedAtDesc(Long orgId, String entityType, Long entityId);

    Page<HrAuditLog> findByOrganizationIdAndEntityTypeOrderByCreatedAtDesc(Long orgId, String entityType, Pageable pageable);
}
