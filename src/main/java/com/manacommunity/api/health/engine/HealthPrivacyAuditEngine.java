package com.manacommunity.api.health.engine;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class HealthPrivacyAuditEngine {
    public static class AuditEntry {
        public String auditId;
        public String accessorUserId;
        public String accessorRole;
        public String patientId;
        public String recordId;
        public String action;
        public Instant timestamp;
        public boolean authorized;
    }

    private final List<AuditEntry> auditTrail = Collections.synchronizedList(new ArrayList<>());
    private final Map<String, Set<String>> patientAuthorizations = new ConcurrentHashMap<>();

    public void grantAccess(String patientId, String authorizedUserId) {
        patientAuthorizations.computeIfAbsent(patientId, k -> ConcurrentHashMap.newKeySet()).add(authorizedUserId);
    }

    public void revokeAccess(String patientId, String authorizedUserId) {
        Set<String> auths = patientAuthorizations.get(patientId);
        if (auths != null) auths.remove(authorizedUserId);
    }

    public boolean checkAndAuditAccess(
            String accessorUserId, String accessorRole, String patientId, String recordId, String action) {
        boolean isOwner = accessorUserId != null && accessorUserId.equals(patientId);
        Set<String> authorizedUsers = patientAuthorizations.getOrDefault(patientId, Collections.emptySet());
        boolean isExplicitlyAuthorized = accessorUserId != null && authorizedUsers.contains(accessorUserId);
        boolean allowed = isOwner || isExplicitlyAuthorized;

        AuditEntry entry = new AuditEntry();
        entry.auditId = UUID.randomUUID().toString();
        entry.accessorUserId = accessorUserId;
        entry.accessorRole = accessorRole;
        entry.patientId = patientId;
        entry.recordId = recordId;
        entry.action = action;
        entry.timestamp = Instant.now();
        entry.authorized = allowed;

        auditTrail.add(entry);
        return allowed;
    }

    public List<AuditEntry> getAuditTrailForPatient(String patientId) {
        List<AuditEntry> res = new ArrayList<>();
        synchronized (auditTrail) {
            for (AuditEntry entry : auditTrail) {
                if (entry.patientId.equals(patientId)) res.add(entry);
            }
        }
        return res;
    }
}