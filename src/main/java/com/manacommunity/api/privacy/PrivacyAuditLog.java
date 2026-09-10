package com.manacommunity.api.privacy;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Audit trail for privacy-sensitive data access and mutations.
 *
 * <p><strong>IMPORTANT:</strong> This table must NEVER contain actual PII values.
 * Only store actor IDs, resource type/ID references, and action metadata.
 * The existence of an entry proves <em>who accessed what</em>, not the data itself.
 */
@Entity
@Table(name = "privacy_audit_log", indexes = {
    @Index(name = "idx_pal_actor",     columnList = "actor_id"),
    @Index(name = "idx_pal_resource",  columnList = "resource_type, resource_id"),
    @Index(name = "idx_pal_timestamp", columnList = "timestamp"),
    @Index(name = "idx_pal_action",    columnList = "action")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PrivacyAuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** ID of the user who performed the action (the actor). */
    @Column(name = "actor_id")
    private Long actorId;

    /** Role of the actor at the time of the action (e.g. ADMIN, SECURITY_GUARD). */
    @Column(name = "actor_role", length = 100)
    private String actorRole;

    /** Privacy action taken (e.g. VIEW_CONTACT_INFO, DELETE_PERSONAL_DATA). */
    @Column(name = "action", nullable = false, length = 80)
    private String action;

    /** Type of resource accessed (e.g. USER, FAMILY_MEMBER, VISITOR_PASS). */
    @Column(name = "resource_type", length = 60)
    private String resourceType;

    /** Database ID of the resource accessed. Never store the PII value itself. */
    @Column(name = "resource_id", length = 40)
    private String resourceId;

    /** Community context for multi-tenant scoping. */
    @Column(name = "community_id")
    private Long communityId;

    /** Optional business reason for the access (if caller provided one). */
    @Column(name = "reason", length = 500)
    private String reason;

    /** HTTP correlation/request ID from MDC — links to the application log. */
    @Column(name = "request_id", length = 64)
    private String requestId;

    /** Client IP address (first hop from X-Forwarded-For or REMOTE_ADDR). */
    @Column(name = "ip_address", length = 50)
    private String ipAddress;

    @Column(name = "timestamp", nullable = false, updatable = false)
    private LocalDateTime timestamp;

    @PrePersist
    protected void onCreate() {
        if (timestamp == null) timestamp = LocalDateTime.now();
    }
}
