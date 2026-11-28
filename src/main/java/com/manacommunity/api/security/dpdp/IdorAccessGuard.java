package com.manacommunity.api.security.dpdp;

import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Defensive Insecure Direct Object Reference (IDOR) Guard.
 * Enforces cross-society tenant isolation and intra-society cross-flat privacy.
 */
@Component
@Slf4j
public class IdorAccessGuard {

    public enum UserRole {
        SUPER_ADMIN,
        SOCIETY_ADMIN,
        GUARD,
        RESIDENT
    }

    @Getter
    @Setter
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class AuthenticatedCaller {
        private String userId;
        private String societyId;
        private String flatNumber;
        private UserRole role;
    }

    @Getter
    @Setter
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class SecurityAuditEvent {
        private String eventType;
        private String callerUserId;
        private String callerSocietyId;
        private String callerFlatNumber;
        private String attemptedSocietyId;
        private String attemptedFlatNumber;
        private Instant timestamp;
        private String reason;
    }

    private final List<SecurityAuditEvent> auditLogs = Collections.synchronizedList(new ArrayList<>());

    public List<SecurityAuditEvent> getAuditLogs() {
        return Collections.unmodifiableList(auditLogs);
    }

    /**
     * Validates that the authenticated caller has legitimate access to the requested target resource.
     * Rejects cross-tenant and cross-flat IDOR tampering attempts.
     */
    public void validateResourceAccess(
            AuthenticatedCaller caller,
            String targetSocietyId,
            String targetFlatNumber) {

        if (caller == null) {
            throw new SecurityException("Unauthenticated request: Caller context missing.");
        }

        // Super-admin has cross-society system access
        if (caller.getRole() == UserRole.SUPER_ADMIN) {
            return;
        }

        // 1. Cross-Society Tenant Isolation Check
        if (targetSocietyId != null && !targetSocietyId.equalsIgnoreCase(caller.getSocietyId())) {
            recordSecurityViolation("IDOR_CROSS_SOCIETY_ATTEMPT", caller, targetSocietyId, targetFlatNumber,
                    "Caller attempted to access resources in foreign society: " + targetSocietyId);
            throw new SecurityException(String.format(
                    "Access denied: User [%s] from society [%s] cannot access resources in foreign society [%s].",
                    caller.getUserId(), caller.getSocietyId(), targetSocietyId));
        }

        // 2. Intra-Society Cross-Flat IDOR Check (Residents can only access their own flat)
        if (caller.getRole() == UserRole.RESIDENT) {
            if (targetFlatNumber != null && !targetFlatNumber.equalsIgnoreCase(caller.getFlatNumber())) {
                recordSecurityViolation("IDOR_CROSS_FLAT_ATTEMPT", caller, targetSocietyId, targetFlatNumber,
                        "Resident attempted to access unauthorized flat: " + targetFlatNumber);
                throw new SecurityException(String.format(
                        "Access denied: Resident [%s] residing in flat [%s] cannot access data for flat [%s].",
                        caller.getUserId(), caller.getFlatNumber(), targetFlatNumber));
            }
        }
    }

    private void recordSecurityViolation(
            String eventType,
            AuthenticatedCaller caller,
            String targetSocietyId,
            String targetFlatNumber,
            String reason) {

        SecurityAuditEvent event = SecurityAuditEvent.builder()
                .eventType(eventType)
                .callerUserId(caller.getUserId())
                .callerSocietyId(caller.getSocietyId())
                .callerFlatNumber(caller.getFlatNumber())
                .attemptedSocietyId(targetSocietyId)
                .attemptedFlatNumber(targetFlatNumber)
                .timestamp(Instant.now())
                .reason(reason)
                .build();

        auditLogs.add(event);
        log.warn("SECURITY ALERT [{}]: User [{}] attempted unauthorized access to Society [{}] / Flat [{}]",
                eventType, caller.getUserId(), targetSocietyId, targetFlatNumber);
    }

    public void clear() {
        auditLogs.clear();
    }
}
