package com.manacommunity.api.mw.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "mw_duty_handovers")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class MwDutyHandover {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long organizationId;
    private Long shiftAssignmentId;

    @Column(name = "handover_from_id")
    private Long handoverFromId;

    @Column(name = "handover_to_id")
    private Long handoverToId;

    @Column(length = 200)
    private String location;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(columnDefinition = "TEXT")
    private String pendingTasks;

    @Column(length = 500)
    private String equipmentStatus;

    private Boolean incidentFlag;

    @Column(columnDefinition = "TEXT")
    private String incidentDesc;

    private Boolean acknowledged;
    private LocalDateTime acknowledgedAt;
    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() {
        createdAt = LocalDateTime.now();
        if (incidentFlag == null) incidentFlag = false;
        if (acknowledged == null) acknowledged = false;
    }
}
