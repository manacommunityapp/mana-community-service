package com.manacommunity.api.hrm.service;

import com.manacommunity.api.hrm.entity.HrAuditLog;
import com.manacommunity.api.hrm.repository.HrAuditLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class HrAuditService {

    private final HrAuditLogRepository auditLogRepository;

    @Transactional
    public HrAuditLog log(Long orgId, Long actorUserId, String actorName,
                          String action, String entityType, Long entityId,
                          String entityDescription, String before, String after) {
        HrAuditLog entry = HrAuditLog.builder()
                .organizationId(orgId)
                .actorUserId(actorUserId)
                .actorName(actorName)
                .action(action)
                .entityType(entityType)
                .entityId(entityId)
                .entityDescription(entityDescription)
                .beforeSnapshot(before)
                .afterSnapshot(after)
                .build();
        return auditLogRepository.save(entry);
    }

    public Page<HrAuditLog> getAuditLogs(Long orgId, Pageable pageable) {
        return auditLogRepository.findByOrganizationIdOrderByCreatedAtDesc(orgId, pageable);
    }

    public Page<HrAuditLog> getAuditLogsByEntityType(Long orgId, String entityType, Pageable pageable) {
        return auditLogRepository.findByOrganizationIdAndEntityTypeOrderByCreatedAtDesc(orgId, entityType, pageable);
    }

    public List<HrAuditLog> getEntityAuditLogs(Long orgId, String entityType, Long entityId) {
        return auditLogRepository.findByOrganizationIdAndEntityTypeAndEntityIdOrderByCreatedAtDesc(orgId, entityType, entityId);
    }
}
