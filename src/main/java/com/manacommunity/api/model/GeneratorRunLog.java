package com.manacommunity.api.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "generator_run_logs")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GeneratorRunLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "generator_id", nullable = false)
    private GeneratorAsset generator;

    @Column(name = "start_time", nullable = false)
    private LocalDateTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalDateTime endTime;

    @Column(name = "duration_hours", nullable = false, precision = 8, scale = 2)
    private BigDecimal durationHours;

    @Column(name = "start_fuel_liters", nullable = false, precision = 10, scale = 2)
    private BigDecimal startFuelLiters;

    @Column(name = "end_fuel_liters", nullable = false, precision = 10, scale = 2)
    private BigDecimal endFuelLiters;

    @Column(name = "diesel_consumed_liters", nullable = false, precision = 10, scale = 2)
    private BigDecimal dieselConsumedLiters;

    @Column(name = "units_generated_kwh", nullable = false, precision = 12, scale = 2)
    private BigDecimal unitsGeneratedKwh;

    @Column(name = "fuel_efficiency_kwh_per_l", precision = 6, scale = 2)
    private BigDecimal fuelEfficiencyKwhPerL;

    @Column(nullable = false, length = 40)
    private String reason; // GRID_OUTAGE, SCHEDULED_TEST_RUN, MAINTENANCE_RUN, PEAK_SHAVING

    @Column(name = "operator_name", length = 120)
    private String operatorName;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (fuelEfficiencyKwhPerL == null && dieselConsumedLiters != null && dieselConsumedLiters.compareTo(BigDecimal.ZERO) > 0 && unitsGeneratedKwh != null) {
            fuelEfficiencyKwhPerL = unitsGeneratedKwh.divide(dieselConsumedLiters, 2, java.math.RoundingMode.HALF_UP);
        }
    }
}
