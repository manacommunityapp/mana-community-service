package com.manacommunity.api.procurement.entity;

import com.manacommunity.api.model.common.BaseAuditEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "proc_maintenance_schedule")
@Getter @Setter @NoArgsConstructor
public class ProcMaintenanceSchedule extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long communityId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "amc_contract_id", nullable = false)
    private ProcAmcContract amcContract;

    @Column(nullable = false)
    private String assetIdentifier;

    @Column(nullable = false)
    private LocalDate scheduledDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ScheduleStatus status = ScheduleStatus.UPCOMING;

    private String technicianName;
    private String technicianPhone;
    private LocalDateTime completedAt;
    private String completionNotes;
    private int sparePartsUsed;
    private String sparePartDetails;
    private String checklistJson; // JSON array of checklist items with done/not-done status

    public enum ScheduleStatus {
        UPCOMING, OVERDUE, IN_PROGRESS, COMPLETED, SKIPPED
    }
}
