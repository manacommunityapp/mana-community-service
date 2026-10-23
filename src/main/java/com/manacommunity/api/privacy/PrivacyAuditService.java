package com.manacommunity.api.privacy;

import com.manacommunity.api.security.CorrelationIdFilter;
import com.manacommunity.api.user.security.UserPrincipal;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Records privacy-sensitive data access and mutations to the {@code privacy_audit_log} table.
 *
 * <p>Separate from the general {@code AuditService} to maintain a focused, immutable
 * privacy audit trail that can be exported for compliance purposes.
 *
 * <p><strong>Rules:</strong>
 * <ul>
 *   <li>Never store actual PII values — only resource type/ID references.</li>
 *   <li>Never throw — audit failures must never interrupt business operations.</li>
 *   <li>Always capture actor, action, resource, IP, and correlation ID.</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
public class PrivacyAuditService {

    private static final Logger AUDIT = LoggerFactory.getLogger("AUDIT");

    private final PrivacyAuditLogRepository repo;

    /**
     * Records a privacy audit event with minimal context.
     *
     * @param action       The action (e.g., "VIEW_CONTACT_INFO", "DELETE_PERSONAL_DATA")
     * @param resourceType The resource type (e.g., "USER", "VISITOR_PASS", "FAMILY_MEMBER")
     * @param resourceId   The resource database ID — never the actual PII value
     */
    public void record(String action, String resourceType, String resourceId) {
        record(action, resourceType, resourceId, null, null);
    }

    /**
     * Records a privacy audit event with community context and optional reason.
     */
    public void record(String action, String resourceType, String resourceId,
                       Long communityId, String reason) {
        try {
            Long actorId = currentActorId();
            String actorRole = currentActorRole();
            String correlationId = MDC.get(CorrelationIdFilter.MDC_CORRELATION_ID);
            String ip = clientIp();

            PrivacyAuditLog entry = PrivacyAuditLog.builder()
                    .actorId(actorId)
                    .actorRole(actorRole)
                    .action(action)
                    .resourceType(resourceType)
                    .resourceId(resourceId)
                    .communityId(communityId)
                    .reason(reason)
                    .requestId(correlationId)
                    .ipAddress(ip)
                    .build();

            repo.save(entry);
            AUDIT.info("PRIVACY action={} resourceType={} resourceId={} actorId={} role={}",
                    action, resourceType, resourceId, actorId, actorRole);

        } catch (Exception ex) {
            // Must NOT re-throw — audit failure must never break business flow
            AUDIT.error("Failed to write privacy audit entry action={} resource={}/{}: {}",
                    action, resourceType, resourceId, ex.getMessage());
        }
    }

    // ── Private helpers ─────────────────────────────────────────────────────

    private Long currentActorId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof UserPrincipal p) {
            return p.getId();
        }
        return null;
    }

    private String currentActorRole() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getAuthorities() != null) {
            return auth.getAuthorities().stream()
                    .map(a -> a.getAuthority())
                    .findFirst()
                    .orElse(null);
        }
        return null;
    }

    private String clientIp() {
        HttpServletRequest req = currentRequest();
        if (req == null) return null;
        String xff = req.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) return xff.split(",")[0].trim();
        return req.getRemoteAddr();
    }

    private HttpServletRequest currentRequest() {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes s) {
            return s.getRequest();
        }
        return null;
    }
}
