package com.manacommunity.api.forensics.entity;

import com.manacommunity.api.forensics.dto.ForensicsDtos.AuditRiskTier;
import com.manacommunity.api.forensics.dto.ForensicsDtos.SensitiveActionType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "worm_audit_events", schema = "forensics")
@Getter
@Setter
public class WormAuditEventEntity {

    @Id
    @Column(name = "audit_id", nullable = false)
    private UUID auditId;

    @Column(name = "society_id", nullable = false)
    private Long societyId;

    @Column(name = "seq_no", nullable = false)
    private Long seqNo;

    @Enumerated(EnumType.STRING)
    @Column(name = "action_type", nullable = false, length = 60)
    private SensitiveActionType actionType;

    @Enumerated(EnumType.STRING)
    @Column(name = "risk_tier", nullable = false, length = 20)
    private AuditRiskTier riskTier;

    @Column(name = "resource_type", nullable = false, length = 50)
    private String resourceType;

    @Column(name = "resource_id", nullable = false, length = 100)
    private String resourceId;

    @Column(name = "actor_id", nullable = false)
    private Long actorId;

    @Column(name = "actor_name", nullable = false, length = 120)
    private String actorName;

    @Column(name = "checker_id")
    private Long checkerId;

    @Column(name = "checker_name", length = 120)
    private String checkerName;

    @Column(name = "reason", nullable = false, length = 300)
    private String reason;

    @Column(name = "old_value_json", nullable = false, columnDefinition = "TEXT")
    private String oldValueJson;

    @Column(name = "new_value_json", nullable = false, columnDefinition = "TEXT")
    private String newValueJson;

    @Column(name = "tombstoned", nullable = false)
    private Boolean tombstoned = Boolean.FALSE;

    @Column(name = "ip_address", nullable = false, length = 64)
    private String ipAddress;

    @Column(name = "correlation_id", nullable = false, length = 100)
    private String correlationId;

    @Column(name = "prev_hash", nullable = false, length = 64)
    private String prevHash;

    @Column(name = "record_hash", nullable = false, length = 64)
    private String recordHash;

    @Column(name = "ecdsa_signature", nullable = false, length = 256)
    private String ecdsaSignature;

    @Column(name = "recorded_at", nullable = false)
    private Instant recordedAt;
}
