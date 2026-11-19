package com.manacommunity.api.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "water_pump_run_logs")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WaterPumpRunLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "pump_id", nullable = false)
    private Long pumpId;

    @Column(name = "start_time", nullable = false)
    private LocalDateTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalDateTime endTime;

    @Column(name = "duration_hours", nullable = false, precision = 8, scale = 2)
    private BigDecimal durationHours;

    @Column(name = "flow_rate_lpm", precision = 8, scale = 2)
    private BigDecimal flowRateLpm;

    @Column(name = "energy_kwh", precision = 8, scale = 2)
    private BigDecimal energyKwh;

    @Column(name = "operator_name", length = 120)
    private String operatorName;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}
