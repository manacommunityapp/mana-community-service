package com.manacommunity.api.parking.ev.entity;

import com.manacommunity.api.model.Community;
import com.manacommunity.api.parking.entity.ResidentVehicle;
import com.manacommunity.api.user.model.AppUser;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "ev_charging_session")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EvChargingSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "community_id", nullable = false)
    private Community community;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "charger_id", nullable = false)
    private EvCharger charger;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "resident_id", nullable = false)
    private AppUser resident;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vehicle_id")
    private ResidentVehicle vehicle;

    @Column(name = "started_at", nullable = false)
    private LocalDateTime startedAt;

    @Column(name = "ended_at")
    private LocalDateTime endedAt;

    @Column(name = "start_meter_kwh", nullable = false)
    private Double startMeterKwh;

    @Column(name = "end_meter_kwh")
    private Double endMeterKwh;

    @Column(name = "total_kwh")
    private Double totalKwh;

    @Column(name = "total_cost", precision = 10, scale = 2)
    private BigDecimal totalCost;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private SessionStatus status = SessionStatus.ACTIVE;

    @Column(name = "cfbos_wallet_transaction_id")
    private Long cfbosWalletTransactionId;

    @Column(name = "stop_reason", length = 100)
    private String stopReason;

    public enum SessionStatus { ACTIVE, COMPLETED, CANCELLED, FAILED }
}