package com.manacommunity.api.mw.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "mw_forecast_snapshots")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class MwForecastSnapshot {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long organizationId;
    private LocalDate weekStartDate;
    private String sourceModule;
    private Integer demandUnits;
    private Integer fulfilledUnits;
    private BigDecimal avgPayRate;
    private Integer workerCount;

    @Column(columnDefinition = "TEXT")
    private String notes;

    private LocalDateTime snapshotAt;

    @PrePersist
    void onCreate() {
        snapshotAt = LocalDateTime.now();
        if (demandUnits == null) demandUnits = 0;
        if (fulfilledUnits == null) fulfilledUnits = 0;
        if (workerCount == null) workerCount = 0;
    }
}
