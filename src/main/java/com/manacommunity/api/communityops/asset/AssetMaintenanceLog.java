package com.manacommunity.api.communityops.asset;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "asset_maintenance_log")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssetMaintenanceLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "asset_id", nullable = false)
    private Long assetId;

    @Column(name = "schedule_id")
    private Long scheduleId;

    @Column(name = "task_description", nullable = false)
    private String taskDescription;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MaintenanceLogStatus status = MaintenanceLogStatus.SCHEDULED;

    @Column(name = "scheduled_date", nullable = false)
    private LocalDate scheduledDate;

    @Column(name = "completed_date")
    private LocalDate completedDate;

    @Column(name = "performed_by")
    private String performedBy;

    @Column(name = "cost_actual", precision = 12, scale = 2)
    private BigDecimal costActual;

    private String notes;

    @Column(name = "reported_by")
    private Long reportedBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();
}
