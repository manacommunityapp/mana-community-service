package com.manacommunity.api.recovery.entity;

import com.manacommunity.api.recovery.dto.RecoveryDtos.GuardrailDecision;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "bulk_mutation_guardrails", schema = "recovery")
@Getter
@Setter
public class BulkMutationGuardrailEntity {

    @Id
    @Column(name = "guard_id", nullable = false)
    private UUID guardId;

    @Column(name = "society_id", nullable = false)
    private Long societyId;

    @Column(name = "operation_name", nullable = false, length = 80)
    private String operationName;

    @Column(name = "requested_rows", nullable = false)
    private Integer requestedRows;

    @Column(name = "canary_rows", nullable = false)
    private Integer canaryRows = 0;

    @Column(name = "gate_drop_pct", nullable = false)
    private Integer gateDropPct = 0;

    @Column(name = "maker_id", nullable = false)
    private Long makerId;

    @Column(name = "checker_id")
    private Long checkerId;

    @Enumerated(EnumType.STRING)
    @Column(name = "decision", nullable = false, length = 40)
    private GuardrailDecision decision;

    @Column(name = "reason", nullable = false, length = 300)
    private String reason;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
}
