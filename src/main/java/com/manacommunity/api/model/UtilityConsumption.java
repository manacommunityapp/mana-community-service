package com.manacommunity.api.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "utility_consumption_summary")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UtilityConsumption {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "unit_id", nullable = false)
    private Long unitId;

    @Column(name = "unit_number", length = 50)
    private String unitNumber;

    @Column(name = "cycle_month", nullable = false, length = 7)
    private String cycleMonth;

    @Column(name = "electricity_kwh")
    private Double electricityKWh;

    @Column(name = "electricity_amount", precision = 10, scale = 2)
    private BigDecimal electricityAmount;

    @Column(name = "water_liters")
    private Double waterLiters;

    @Column(name = "water_amount", precision = 10, scale = 2)
    private BigDecimal waterAmount;

    @Column(name = "dg_backup_kwh")
    private Double dgBackupKWh;

    @Column(name = "dg_backup_amount", precision = 10, scale = 2)
    private BigDecimal dgBackupAmount;

    @Column(name = "total_utility_amount", precision = 10, scale = 2)
    private BigDecimal totalUtilityAmount;

    @Column(name = "cfbos_sync_status", length = 10)
    private String cfbosSyncStatus;

    @PrePersist
    protected void onCreate() {
        if (cfbosSyncStatus == null) cfbosSyncStatus = "PENDING";
        if (electricityKWh == null) electricityKWh = 0.0;
        if (waterLiters == null) waterLiters = 0.0;
        if (dgBackupKWh == null) dgBackupKWh = 0.0;
        if (electricityAmount == null) electricityAmount = BigDecimal.ZERO;
        if (waterAmount == null) waterAmount = BigDecimal.ZERO;
        if (dgBackupAmount == null) dgBackupAmount = BigDecimal.ZERO;
        if (totalUtilityAmount == null) totalUtilityAmount = BigDecimal.ZERO;
    }
}
