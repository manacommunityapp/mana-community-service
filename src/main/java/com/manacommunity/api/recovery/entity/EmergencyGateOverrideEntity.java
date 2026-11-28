package com.manacommunity.api.recovery.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "emergency_gate_overrides", schema = "recovery")
@Getter
@Setter
public class EmergencyGateOverrideEntity {

    @Id
    @Column(name = "override_id", nullable = false)
    private UUID overrideId;

    @Column(name = "society_id", nullable = false)
    private Long societyId;

    @Column(name = "feature_key", nullable = false, length = 80)
    private String featureKey;

    @Column(name = "enabled", nullable = false)
    private Boolean enabled = Boolean.FALSE;

    @Column(name = "deactivated_since", nullable = false)
    private Instant deactivatedSince;

    @Column(name = "max_rows_guardrail", nullable = false)
    private Integer maxRowsGuardrail = 50;

    @Column(name = "canary_abort_threshold_pct", nullable = false)
    private Integer canaryAbortThresholdPct = 5;

    @Column(name = "activated_by", nullable = false, length = 100)
    private String activatedBy;

    @Column(name = "activated_at", nullable = false)
    private Instant activatedAt;

    @Column(name = "deactivated_at")
    private Instant deactivatedAt;
}
