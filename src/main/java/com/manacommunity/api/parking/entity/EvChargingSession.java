package com.manacommunity.api.parking.entity;

import com.manacommunity.api.model.BaseAuditEntity;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "ev_charging_sessions")
@Data
@EqualsAndHashCode(callSuper = true)
public class EvChargingSession extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "community_id", nullable = false)
    private Long communityId;

    @Column(name = "charger_id", nullable = false)
    private Long chargerId;

    @Column(name = "flat_number", nullable = false)
    private String flatNumber;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "vehicle_number", nullable = false)
    private String vehicleNumber;

    @Column(nullable = false)
    private String status; // QUEUED, CHARGING, COMPLETED, AUTO_CUTOFF, OVERSTAY

    @Column(name = "queue_position")
    private Integer queuePosition;

    @Column(name = "start_time")
    private Instant startTime;

    @Column(name = "end_time")
    private Instant endTime;

    @Column(name = "energy_kwh")
    private BigDecimal energyKwh = BigDecimal.ZERO;

    @Column(name = "energy_cost")
    private BigDecimal energyCost = BigDecimal.ZERO;

    @Column(name = "overstay_minutes")
    private Integer overstayMinutes = 0;

    @Column(name = "overstay_penalty")
    private BigDecimal overstayPenalty = BigDecimal.ZERO;

    @Column(name = "total_amount")
    private BigDecimal totalAmount = BigDecimal.ZERO;

    @Column(name = "billed_to_maintenance")
    private Boolean billedToMaintenance = false;

    @Column(name = "ocpp_transaction_id")
    private String ocppTransactionId;

    @Column(name = "max_session_minutes")
    private Integer maxSessionMinutes;
}
