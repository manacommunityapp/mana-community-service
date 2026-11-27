package com.manacommunity.api.opsresilience.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(
    name = "tenant_accounting_rules",
    uniqueConstraints = @UniqueConstraint(name = "uq_tenant_accounting_rule", columnNames = {"community_id", "rule_key"})
)
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class TenantAccountingRule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "community_id", nullable = false)
    private Long communityId;

    @Column(name = "rule_key", nullable = false, length = 64)
    private String ruleKey;

    @Column(name = "rule_type", nullable = false, length = 32)
    private String ruleType; // PENALTY_WAIVER, GST_OVERRIDE, SINKING_FUND_SURCHARGE, LATE_FEE_CAP

    @Builder.Default
    @Column(name = "rate_bps", nullable = false)
    private int rateBps = 0;

    @Builder.Default
    @Column(name = "flat_amount_paise", nullable = false)
    private long flatAmountPaise = 0L;

    @Column(name = "effective_from", nullable = false)
    private Instant effectiveFrom;

    @Column(name = "effective_to", nullable = false)
    private Instant effectiveTo;

    @Builder.Default
    @Column(name = "dry_run_only", nullable = false)
    private boolean dryRunOnly = true;

    @Builder.Default
    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "audit_reason", length = 255)
    private String auditReason;

    @Builder.Default
    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();
}
