package com.manacommunity.api.health.entity;

import com.manacommunity.api.model.common.BaseAuditEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "health_benefit_usage", schema = "manacommunity")
@Data
@EqualsAndHashCode(callSuper = false)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HealthBenefitUsage extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @Column(name = "resident_user_id", nullable = false)
    private Long residentUserId;

    @Column(name = "community_allowance", nullable = false, precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal communityAllowance = new BigDecimal("5000.00");

    @Column(name = "used_allowance", nullable = false, precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal usedAllowance = BigDecimal.ZERO;

    @Column(name = "cashless_active", nullable = false)
    @Builder.Default
    private Boolean cashlessActive = true;
}
