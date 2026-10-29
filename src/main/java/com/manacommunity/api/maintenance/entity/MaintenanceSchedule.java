package com.manacommunity.api.maintenance.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "maintenance_schedule")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MaintenanceSchedule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "community_id", nullable = false)
    private Long communityId;

    @Column(length = 200, nullable = false)
    private String title;

    @Column(length = 50, nullable = false)
    @Builder.Default
    private String category = "GENERAL";

    @Column(name = "recurrence_type", length = 20, nullable = false)
    @Builder.Default
    private String recurrenceType = "MONTHLY";

    @Column(name = "next_due_date", nullable = false)
    private LocalDate nextDueDate;

    @Column(name = "last_done_date")
    private LocalDate lastDoneDate;

    @Column(name = "last_run_date")
    private LocalDate lastRunDate;

    @Column(name = "assigned_vendor_id")
    private Long assignedVendorId;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at", nullable = false)
    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
