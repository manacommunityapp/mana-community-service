package com.manacommunity.api.mw.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "mw_forecast_configs")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class MwForecastConfig {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long organizationId;
    private Integer horizonWeeks;
    private Integer minWorkersPerShift;
    private BigDecimal peakDayMultiplier;
    private String sourceModules;
    private Boolean autoSnapshotEnabled;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @PrePersist
    void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (horizonWeeks == null) horizonWeeks = 4;
        if (minWorkersPerShift == null) minWorkersPerShift = 1;
        if (peakDayMultiplier == null) peakDayMultiplier = new BigDecimal("1.5");
        if (sourceModules == null) sourceModules = "POOL,JOBS";
        if (autoSnapshotEnabled == null) autoSnapshotEnabled = true;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
