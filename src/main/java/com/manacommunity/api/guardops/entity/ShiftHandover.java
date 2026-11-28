package com.manacommunity.api.guardops.entity;

import com.manacommunity.api.model.common.BaseAuditEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "shift_handovers")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ShiftHandover extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String communityId;
    private Long outgoingGuardId;
    private String outgoingGuardName;
    private Long incomingGuardId;
    private String incomingGuardName;
    private String gateId;
    private String gateName;

    private LocalDateTime handoverInitiatedAt;
    private LocalDateTime handoverCompletedAt;

    private int visitorsStillInside;
    private int uncollectedPackages;
    private int openIncidents;
    private int pendingDeliveries;
    private int brokenEquipmentCount;

    @Column(length = 2000)
    private String openItemsSummary;

    private boolean outgoingAcknowledged;
    private boolean incomingAcknowledged;
    private boolean supervisorVerified;
    private String supervisorName;

    private String outgoingSelfieUrl;
    private String incomingSelfieUrl;
    private boolean outgoingBiometricVerified;
    private boolean incomingBiometricVerified;

    @Enumerated(EnumType.STRING)
    private HandoverStatus status;

    @Column(length = 1000)
    private String remarks;

    public enum HandoverStatus { INITIATED, CHECKLIST_PENDING, INCOMING_REVIEW, ACKNOWLEDGED, SUPERVISOR_VERIFIED, COMPLETED, DISPUTED }
}
