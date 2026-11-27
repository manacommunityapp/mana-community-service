package com.manacommunity.api.handover.entity;

import com.manacommunity.api.model.common.BaseAuditEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "snag_items")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class SnagItem extends BaseAuditEntity {

    public enum SnagCategory {
        CIVIL, PLUMBING, ELECTRICAL, PAINTING, CARPENTRY, WATERPROOFING,
        FLOORING, GLASS_ALUMINUM, HVAC, FIRE_SAFETY, ELEVATOR, LANDSCAPING
    }

    public enum SnagSeverity { CRITICAL, MAJOR, MINOR, COSMETIC }

    public enum SnagStatus {
        LOGGED, ACKNOWLEDGED, ASSIGNED, WORK_IN_PROGRESS, REWORK_NEEDED,
        RE_INSPECTION_PENDING, RE_INSPECTION_PASSED, CLOSED, DISPUTED
    }

    public enum RoomLocation {
        LIVING_ROOM, MASTER_BEDROOM, BEDROOM_2, BEDROOM_3, KITCHEN,
        BATHROOM_MASTER, BATHROOM_COMMON, BALCONY, UTILITY, ENTRANCE_FOYER,
        CORRIDOR, PARKING, TERRACE, COMMON_AREA, STAIRCASE, LOBBY
    }

    @Column(nullable = false) private Long communityId;
    @Column(nullable = false) private Long projectId;

    @Column(nullable = false) private String flatNumber;
    @Column(nullable = false) private String towerName;
    private int floorNumber;

    @Column(nullable = false, unique = true) private String snagCode;

    @Enumerated(EnumType.STRING) @Column(nullable = false) private SnagCategory category;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private SnagSeverity severity;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private SnagStatus status;
    @Enumerated(EnumType.STRING) private RoomLocation roomLocation;

    private String exactCoordinates;
    @Column(length = 1000) private String description;

    private String photoBeforeUrl;
    private String annotatedPhotoUrl;
    private String photoAfterUrl;

    private String reportedByName;
    private String reportedByPhone;
    private LocalDateTime reportedAt;

    private String assignedSubcontractorId;
    private String assignedSubcontractorName;
    private String subcontractorTrade;
    private LocalDateTime assignedAt;

    private LocalDate targetResolutionDate;
    private LocalDateTime resolvedAt;
    private LocalDateTime reInspectionDate;
    private String reInspectedByName;

    private int reworkCount;
    private int reInspectionCount;

    @Column(length = 500) private String resolutionNotes;
    @Column(length = 500) private String disputeReason;

    @Builder.Default private boolean possessionBlocker = false;
    @Builder.Default private boolean buyerAcknowledged = false;
    private LocalDateTime buyerAcknowledgedAt;
}
