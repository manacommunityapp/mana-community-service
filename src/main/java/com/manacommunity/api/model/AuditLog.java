package com.manacommunity.api.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Durable audit trail row — one record per sensitive mutation (who did what,
 * to which entity, with before/after values, from where).
 *
 * <p>{@code action} and {@code module} are stored as plain strings (not
 * {@code @Enumerated}) on purpose: a future rename/removal of an enum constant
 * must never break reads of historical rows.</p>
 */
@Entity
@Table(
    name = "audit_log",
    indexes = {
        @Index(name = "idx_audit_user", columnList = "user_id, created_at"),
        @Index(name = "idx_audit_module", columnList = "module, created_at"),
        @Index(name = "idx_audit_action", columnList = "action, created_at")
    }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 1. TENANT */
    @Column(name = "tenant_id", length = 64)
    private String tenantId;

    /** 2. WHO */
    @Column(name = "user_id")
    private Long userId;

    @Column(name = "username", length = 100)
    private String username;

    @Column(name = "user_role", length = 50)
    private String userRole;

    /** 3. WHAT */
    @Column(nullable = false, length = 80)
    private String action;

    @Column(nullable = false, length = 50)
    private String module;

    /** 4. WHEN */
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /** 5. WHERE */
    @Column(name = "service_name", length = 60)
    private String serviceName;

    @Column(name = "endpoint", length = 150)
    private String endpoint;

    @Column(name = "location", length = 100)
    private String location;

    /** 6. RESOURCE */
    @Column(name = "entity_name", length = 80)
    private String entityName; // Resource type (e.g. PAYMENT, VOTE, USER, ROLE, VISITOR_PASS, etc.)

    @Column(name = "entity_id", length = 64)
    private String entityId; // Resource ID

    /** 7. OLD VALUE */
    @Column(name = "old_value", columnDefinition = "text")
    private String oldValue;

    /** 8. NEW VALUE */
    @Column(name = "new_value", columnDefinition = "text")
    private String newValue;

    /** 9. IP */
    @Column(name = "ip_address", length = 64)
    private String ipAddress;

    /** 10. DEVICE */
    @Column(name = "device_info", length = 120)
    private String deviceInfo;

    @Column(name = "user_agent", length = 255)
    private String userAgent;

    /** 11. CORRELATION ID */
    @Column(name = "correlation_id", length = 64)
    private String correlationId;

    /** ── TAMPER-RESISTANT CRYPTOGRAPHIC INTEGRITY CHAIN ── */
    @Column(name = "sequence_number")
    private Long sequenceNumber;

    @Column(name = "prev_hash", length = 64)
    private String prevHash;

    @Column(name = "record_hash", length = 64)
    private String recordHash;

    @Column(name = "signature", length = 128)
    private String signature;

    @PrePersist
    void onCreate() {
        if (createdAt == null) createdAt = LocalDateTime.now();
    }
}
