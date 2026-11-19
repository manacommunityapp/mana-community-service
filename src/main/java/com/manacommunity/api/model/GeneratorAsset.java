package com.manacommunity.api.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "generator_assets",
        uniqueConstraints = @UniqueConstraint(name = "uk_gen_serial", columnNames = {"serial_number"}))
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GeneratorAsset {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "community_id", nullable = false)
    private Long communityId;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(name = "make_model", nullable = false, length = 120)
    private String makeModel;

    @Column(name = "serial_number", nullable = false, length = 80)
    private String serialNumber;

    @Column(name = "capacity_kva", nullable = false)
    private Integer capacityKva;

    @Column(length = 150)
    private String location;

    @Column(name = "fuel_tank_capacity_liters", nullable = false, precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal fuelTankCapacityLiters = BigDecimal.valueOf(500);

    @Column(name = "current_fuel_liters", nullable = false, precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal currentFuelLiters = BigDecimal.ZERO;

    @Column(name = "total_run_hours", nullable = false, precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal totalRunHours = BigDecimal.ZERO;

    @Column(name = "last_service_hours", nullable = false, precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal lastServiceHours = BigDecimal.ZERO;

    @Column(name = "service_interval_hours", nullable = false)
    @Builder.Default
    private Integer serviceIntervalHours = 250;

    @Column(name = "battery_voltage", precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal batteryVoltage = BigDecimal.valueOf(12.6);

    @Column(name = "coolant_temp_celsius")
    @Builder.Default
    private Integer coolantTempCelsius = 65;

    @Column(name = "oil_pressure_bar", precision = 4, scale = 2)
    @Builder.Default
    private BigDecimal oilPressureBar = BigDecimal.valueOf(4.0);

    @Column(nullable = false, length = 30)
    @Builder.Default
    private String status = "STANDBY"; // STANDBY, RUNNING, MAINTENANCE, FAULT

    @Column(name = "amc_vendor", length = 150)
    private String amcVendor;

    @Column(name = "amc_expiry_date")
    private LocalDate amcExpiryDate;

    @Column(name = "meter_serial_number", length = 100)
    private String meterSerialNumber;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (fuelTankCapacityLiters == null) fuelTankCapacityLiters = BigDecimal.valueOf(500);
        if (currentFuelLiters == null) currentFuelLiters = BigDecimal.ZERO;
        if (totalRunHours == null) totalRunHours = BigDecimal.ZERO;
        if (lastServiceHours == null) lastServiceHours = BigDecimal.ZERO;
        if (serviceIntervalHours == null) serviceIntervalHours = 250;
        if (status == null) status = "STANDBY";
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
