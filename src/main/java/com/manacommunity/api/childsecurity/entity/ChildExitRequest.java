package com.manacommunity.api.childsecurity.entity;

import com.manacommunity.api.model.common.BaseAuditEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "child_exit_requests")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ChildExitRequest extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long childId;
    private String childName;
    private String communityId;
    private String flatNumber;
    private String towerName;

    @Enumerated(EnumType.STRING)
    private ExitType exitType;

    @Enumerated(EnumType.STRING)
    private ExitRequestStatus status;

    @Enumerated(EnumType.STRING)
    private EscalationState escalationState;

    private String guardOnDuty;
    private String gateId;

    private LocalDateTime requestedAt;
    private LocalDateTime primaryNotifiedAt;
    private LocalDateTime escalatedAt;
    private LocalDateTime ivrTriggeredAt;
    private LocalDateTime approvedAt;
    private LocalDateTime deniedAt;
    private LocalDateTime expiredAt;

    private Long approvedByGuardianId;
    private String approvedByName;
    private String approvalMethod;

    private String companionName;
    private String companionPhone;
    private String companionRelation;
    private String companionPhotoUrl;

    private int escalationTimeoutSeconds;
    private int totalEscalationAttempts;
    private String remarks;

    public enum ExitType { UNACCOMPANIED, WITH_GUARDIAN, WITH_AUTHORIZED_PICKUP, SCHOOL_BUS, EMERGENCY }

    public enum ExitRequestStatus { PENDING, APPROVED, DENIED, EXPIRED, AUTO_LOCKED, EMERGENCY_OVERRIDE }

    public enum EscalationState {
        AWAITING_PRIMARY,
        PRIMARY_TIMEOUT_ESCALATE_BACKUP,
        IVR_TRIGGERED,
        IVR_CONNECTED,
        GUARDIAN_RESPONDED,
        ALL_ESCALATIONS_EXHAUSTED,
        AUTO_DENY
    }
}
