package com.manacommunity.api.assetaudit.entity;

import com.manacommunity.api.model.common.BaseAuditEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.time.LocalTime;

@Entity
@Table(name = "housekeeping_tasks")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class HousekeepingTask extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long communityId;
    private String locationId;
    private String locationName;

    @Enumerated(EnumType.STRING)
    private LocationType locationType;

    private String towerName;
    private String floor;
    private String qrCode;

    @Enumerated(EnumType.STRING)
    private TaskStatus status;

    @Enumerated(EnumType.STRING)
    private TaskPriority priority;

    private String assignedStaffId;
    private String assignedStaffName;
    private String supervisorId;

    private LocalTime scheduledTime;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
    private long durationMinutes;

    private String checklistJson;
    private String completedItemsJson;
    private String photoBeforeUrl;
    private String photoAfterUrl;

    private boolean residentReported;
    private String residentReportQrScan;
    private LocalDateTime residentReportedAt;
    private String residentRemarks;

    private boolean escalated;
    private String escalationReason;
    private LocalDateTime escalatedAt;
    private int escalationLevel;

    private double ratingScore;
    private String ratingRemarks;

    public enum LocationType {
        TOWER_LOBBY, CLUBHOUSE_RESTROOM, GYM, POOL_AREA, GARDEN,
        PARKING_LOT, PLAY_AREA, COMMUNITY_HALL, TERRACE, STAIRWELL
    }

    public enum TaskStatus {
        SCHEDULED, ASSIGNED, IN_PROGRESS, COMPLETED, VERIFIED,
        REPORTED_DIRTY, RE_ASSIGNED, ESCALATED, SKIPPED
    }

    public enum TaskPriority {
        LOW, NORMAL, HIGH, URGENT, CRITICAL
    }
}
