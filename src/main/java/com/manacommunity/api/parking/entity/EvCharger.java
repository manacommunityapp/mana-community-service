package com.manacommunity.api.parking.entity;

import com.manacommunity.api.model.BaseAuditEntity;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "ev_chargers")
@Data
@EqualsAndHashCode(callSuper = true)
public class EvCharger extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "community_id", nullable = false)
    private Long communityId;

    @Column(name = "charger_code", nullable = false, unique = true)
    private String chargerCode; // OCPP charger identity

    @Column(name = "power_kw", nullable = false)
    private BigDecimal powerKw; // e.g. 7.4

    @Column(nullable = false)
    private String status; // AVAILABLE, CHARGING, RESERVED, FAULTED, OFFLINE

    @Column(name = "current_session_id")
    private Long currentSessionId;

    @Column(name = "bay_id")
    private Long bayId;

    @Column(name = "connector_type")
    private String connectorType; // Type2, CCS2, CHAdeMO

    @Column(name = "max_session_minutes")
    private Integer maxSessionMinutes = 240; // 4 hours default

    @Column(name = "overstay_penalty_per_hour")
    private BigDecimal overstayPenaltyPerHour = BigDecimal.valueOf(50);

    @Column(name = "rate_per_kwh")
    private BigDecimal ratePerKwh = BigDecimal.valueOf(12);

    @Version
    private Long version;
}
