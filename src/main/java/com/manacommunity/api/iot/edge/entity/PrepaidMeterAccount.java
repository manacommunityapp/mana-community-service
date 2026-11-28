package com.manacommunity.api.iot.edge.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "prepaid_meter_accounts", uniqueConstraints = {
    @UniqueConstraint(name = "uq_prepaid_meter", columnNames = {"meter_id"})
})
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class PrepaidMeterAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "meter_id", nullable = false, unique = true)
    private Long meterId;

    @Column(name = "community_id", nullable = false)
    private Long communityId;

    @Column(name = "unit_number", length = 50)
    private String unitNumber;

    @Column(name = "balance", nullable = false, precision = 14, scale = 4)
    @Builder.Default
    private BigDecimal balance = BigDecimal.ZERO;

    @Column(name = "low_balance_threshold", nullable = false, precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal lowBalanceThreshold = new BigDecimal("100.00");

    @Column(name = "rate_per_unit", nullable = false, precision = 10, scale = 4)
    @Builder.Default
    private BigDecimal ratePerUnit = new BigDecimal("8.00");

    @Column(name = "dg_rate_per_unit", nullable = false, precision = 10, scale = 4)
    @Builder.Default
    private BigDecimal dgRatePerUnit = new BigDecimal("22.00");

    @Column(name = "active_tariff_source", nullable = false, length = 20)
    @Builder.Default
    private String activeTariffSource = "GRID";

    @Column(name = "last_deduction_at")
    private Instant lastDeductionAt;

    @Column(name = "last_sequence_number", nullable = false)
    @Builder.Default
    private Long lastSequenceNumber = 0L;

    @Column(name = "total_consumed", nullable = false, precision = 14, scale = 4)
    @Builder.Default
    private BigDecimal totalConsumed = BigDecimal.ZERO;

    @Column(name = "low_balance_alert_sent", nullable = false)
    @Builder.Default
    private Boolean lowBalanceAlertSent = false;

    @Column(name = "is_disconnected", nullable = false)
    @Builder.Default
    private Boolean isDisconnected = false;

    @Column(nullable = false)
    @Builder.Default
    private Boolean active = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();

    @Version
    private Long version;
}
