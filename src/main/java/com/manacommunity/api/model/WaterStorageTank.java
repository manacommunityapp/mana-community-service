package com.manacommunity.api.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "water_storage_tanks")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WaterStorageTank {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "community_id", nullable = false)
    private Long communityId;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(name = "tank_type", nullable = false, length = 50)
    private String tankType; // UG_RAW, UG_TREATED, UG_SOFT, UG_FIRE_RESERVE, OHT_TOWER_A, OHT_TOWER_B, OHT_TOWER_C

    @Column(length = 150)
    private String location;

    @Column(name = "capacity_liters", nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal capacityLiters = BigDecimal.valueOf(100000);

    @Column(name = "current_level_liters", nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal currentLevelLiters = BigDecimal.valueOf(50000);

    @Column(name = "low_level_threshold_liters", nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal lowLevelThresholdLiters = BigDecimal.valueOf(25000);

    @Column(name = "sensor_status", nullable = false, length = 30)
    @Builder.Default
    private String sensorStatus = "ONLINE"; // ONLINE, OFFLINE, CALIBRATING

    @Column(name = "last_telemetry_time")
    private LocalDateTime lastTelemetryTime;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at", nullable = false)
    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();

    @PreUpdate
    public void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
