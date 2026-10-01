package com.manacommunity.api.parking.ev.entity;

import com.manacommunity.api.parking.ev.enums.EvConnectorType;
import com.manacommunity.api.parking.ev.enums.EvStationStatus;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "ev_charging_stations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EvChargingStation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 64)
    private String hardwareId; // e.g. "CHARGER-TOWER-A-01"

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false)
    private Long communityId;

    private Long parkingSlotId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    @Builder.Default
    private EvConnectorType connectorType = EvConnectorType.TYPE_2_AC;

    @Column(nullable = false, precision = 6, scale = 2)
    @Builder.Default
    private BigDecimal maxPowerKw = new BigDecimal("7.40");

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    @Builder.Default
    private EvStationStatus status = EvStationStatus.AVAILABLE;

    @Column(precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal currentPowerKw = BigDecimal.ZERO;

    @Column(precision = 12, scale = 3)
    @Builder.Default
    private BigDecimal latestMeterKwh = BigDecimal.ZERO;

    private LocalDateTime lastHeartbeatAt;

    private String firmwareVersion;

    private String lastFaultCode;

    @Column(nullable = false)
    @Builder.Default
    private Boolean active = true;

    @Column(nullable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    private LocalDateTime updatedAt;

    @PreUpdate
    public void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
