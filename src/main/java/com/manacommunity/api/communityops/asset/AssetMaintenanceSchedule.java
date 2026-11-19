package com.manacommunity.api.communityops.asset;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "asset_maintenance_schedule")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssetMaintenanceSchedule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "asset_id", nullable = false)
    private Long assetId;

    @Column(name = "task_name", nullable = false)
    private String taskName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MaintenanceFrequency frequency;

    @Column(name = "last_done")
    private LocalDate lastDone;

    @Column(name = "next_due", nullable = false)
    private LocalDate nextDue;

    @Column(name = "assigned_vendor")
    private String assignedVendor;

    @Column(name = "estimated_cost", precision = 12, scale = 2)
    private BigDecimal estimatedCost;

    private String notes;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();
}
