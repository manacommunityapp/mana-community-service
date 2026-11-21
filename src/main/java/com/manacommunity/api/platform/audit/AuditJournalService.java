package com.manacommunity.api.platform.audit;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.manacommunity.api.platform.web.CorrelationContext;
import com.manacommunity.api.user.security.UserPrincipal;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Consolidated audit entry point for all five audit categories.
 *
 * <strong>Financial invariant</strong>: {@link #recordFinancial} uses
 * {@code Propagation.MANDATORY}, meaning it MUST run inside an existing
 * transaction. If the journal persist fails, the exception propagates and the
 * calling financial operation rolls back. A financial approval, refund or
 * settlement must never complete without its audit record.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuditJournalService {

    private final AuditJournalRepository repository;
    private final ObjectMapper objectMapper;

    public void recordBusiness(String action, String entityType, String entityId,
                                Long communityId, Object details) {
        persist(AuditCategory.BUSINESS, action, entityType, entityId, communityId,
                currentActorId(), "USER", toJson(details), clientIp());
    }

    public void recordSecurity(String action, Long actorId, String ipAddress, Object details) {
        persist(AuditCategory.SECURITY, action, null, null, null,
                actorId, "USER", toJson(details), ipAddress);
    }

    public void recordPrivacy(String action, Long actorId, String entityType, String entityId,
                               Long communityId) {
        persist(AuditCategory.PRIVACY, action, entityType, entityId, communityId,
                actorId, "USER", null, clientIp());
    }

    /**
     * Records a financial audit event.
     * Uses MANDATORY propagation — the caller MUST already be in a transaction.
     * If the persist fails, the exception propagates so the financial operation rolls back.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public void recordFinancial(String action, String entityType, String entityId,
                                 Long communityId, Object details) {
        persist(AuditCategory.FINANCIAL, action, entityType, entityId, communityId,
                currentActorId(), "USER", toJson(details), clientIp());
    }

    public void recordOperational(String action, Object details) {
        persist(AuditCategory.OPERATIONAL, action, null, null, null,
                null, "SYSTEM", toJson(details), null);
    }

    private void persist(AuditCategory category, String action, String entityType, String entityId,
                          Long communityId, Long actorId, String actorType,
                          String details, String ipAddress) {
        AuditJournalEntry entry = AuditJournalEntry.builder()
                .category(category)
                .action(action)
                .entityType(entityType)
                .entityId(entityId)
                .communityId(communityId)
                .actorId(actorId)
                .actorType(actorType)
                .details(details)
                .correlationId(CorrelationContext.get())
                .ipAddress(ipAddress)
                .build();
        repository.save(entry);
    }

    private Long currentActorId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof UserPrincipal principal) {
            return principal.getId();
        }
        return null;
    }

    private String clientIp() {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attrs) {
            HttpServletRequest req = attrs.getRequest();
            String xff = req.getHeader("X-Forwarded-For");
            if (xff != null && !xff.isBlank()) return xff.split(",")[0].trim();
            return req.getRemoteAddr();
        }
        return null;
    }

    private String toJson(Object obj) {
        if (obj == null) return null;
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            return "{\"error\":\"serialization failed\"}";
        }
    }
}
