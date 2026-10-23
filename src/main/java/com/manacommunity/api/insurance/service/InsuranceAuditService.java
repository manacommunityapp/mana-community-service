package com.manacommunity.api.insurance.service;

import com.manacommunity.api.insurance.entity.InsuranceAuditEvent;
import com.manacommunity.api.insurance.repository.InsuranceAuditEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HexFormat;
import java.util.List;

/**
 * Tamper-evident audit trail. Each event embeds the hash of its predecessor. Runs in the caller's transaction,
 * so a failed audit write rolls back the business change (we do not swallow audit failures here).
 * {@code detail} must never contain PII, health data or secrets.
 */
@Service
@RequiredArgsConstructor
public class InsuranceAuditService {

    private final InsuranceAuditEventRepository repo;

    @Transactional
    public synchronized InsuranceAuditEvent record(Long actorUserId, String action, String entityType,
                                                   String entityId, String detail) {
        String prev = repo.findFirstByOrderByIdDesc().map(InsuranceAuditEvent::getHash).orElse(null);
        LocalDateTime now = LocalDateTime.now().truncatedTo(ChronoUnit.MICROS);
        String safeDetail = detail == null ? null : detail.substring(0, Math.min(detail.length(), 500));
        String hash = computeHash(prev, actorUserId, action, entityType, entityId, safeDetail, now);
        return repo.save(InsuranceAuditEvent.builder()
                .actorUserId(actorUserId).action(action).entityType(entityType).entityId(entityId)
                .detail(safeDetail).prevHash(prev).hash(hash).createdAt(now).build());
    }

    /** True when every event's stored hash recomputes and links to its predecessor. */
    public static boolean verifyChain(List<InsuranceAuditEvent> orderedById) {
        String prev = null;
        for (InsuranceAuditEvent e : orderedById) {
            String expected = computeHash(prev, e.getActorUserId(), e.getAction(), e.getEntityType(),
                    e.getEntityId(), e.getDetail(), e.getCreatedAt());
            if (!expected.equals(e.getHash())) return false;
            if (prev != null && !prev.equals(e.getPrevHash())) return false;
            prev = e.getHash();
        }
        return true;
    }

    public static String computeHash(String prev, Long actor, String action, String entityType, String entityId,
                                     String detail, LocalDateTime ts) {
        String material = String.join("|", prev == null ? "" : prev, String.valueOf(actor), action, entityType,
                entityId == null ? "" : entityId, detail == null ? "" : detail, String.valueOf(ts));
        try {
            return HexFormat.of().formatHex(
                    MessageDigest.getInstance("SHA-256").digest(material.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
