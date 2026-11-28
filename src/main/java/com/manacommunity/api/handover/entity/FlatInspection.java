package com.manacommunity.api.handover.entity;

import com.manacommunity.api.model.common.BaseAuditEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "flat_inspections")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class FlatInspection extends BaseAuditEntity {

    public enum InspectionPhase { PRE_POSSESSION, FINAL_WALKTHROUGH, RE_INSPECTION, HANDOVER_SIGN_OFF }
    public enum InspectionOutcome { PASS, CONDITIONAL_PASS, FAIL, PENDING }

    @Column(nullable = false) private Long communityId;
    @Column(nullable = false) private Long projectId;

    @Column(nullable = false) private String flatNumber;
    @Column(nullable = false) private String towerName;
    private int floorNumber;

    @Enumerated(EnumType.STRING) @Column(nullable = false) private InspectionPhase phase;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private InspectionOutcome outcome;

    private String inspectorName;
    private String inspectorRole;
    private String buyerName;
    private String buyerPhone;

    private LocalDateTime scheduledAt;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
    private long durationMinutes;

    private int totalSnagsFound;
    private int criticalSnagsOpen;
    private int majorSnagsOpen;
    private int minorSnagsOpen;

    @Builder.Default private boolean possessionSignOffBlocked = false;
    @Builder.Default private boolean buyerSignedOff = false;
    private LocalDateTime buyerSignOffAt;
    private String buyerSignatureUrl;

    @Column(length = 1000) private String remarks;
    private String inspectionReportUrl;
}
