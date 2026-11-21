package com.manacommunity.api.platform.audit;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "audit_journal")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditJournalEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "audit_id", nullable = false, updatable = false)
    @Builder.Default
    private UUID auditId = UUID.randomUUID();

    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false, columnDefinition = "audit_category")
    private AuditCategory category;

    @Column(name = "community_id")
    private Long communityId;

    @Column(name = "actor_id")
    private Long actorId;

    @Column(name = "actor_type", length = 50)
    private String actorType;

    @Column(name = "action", nullable = false, length = 200)
    private String action;

    @Column(name = "entity_type", length = 100)
    private String entityType;

    @Column(name = "entity_id", length = 255)
    private String entityId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "details", columnDefinition = "jsonb")
    private String details;

    @Column(name = "correlation_id", length = 100)
    private String correlationId;

    @Column(name = "ip_address", columnDefinition = "inet")
    private String ipAddress;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();
}
