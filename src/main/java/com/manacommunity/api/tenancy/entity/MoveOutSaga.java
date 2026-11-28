package com.manacommunity.api.tenancy.entity;

import com.manacommunity.api.model.common.BaseAuditEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "move_out_sagas")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class MoveOutSaga extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long tenantRecordId;
    private String flatNumber;
    private String towerName;

    @Enumerated(EnumType.STRING)
    private SagaStatus overallStatus;

    // Step 1: Dues verification
    @Enumerated(EnumType.STRING)
    private StepStatus duesVerification;
    private double pendingAmount;
    private LocalDateTime duesCheckedAt;

    // Step 2: App access revocation & guest passcodes
    @Enumerated(EnumType.STRING)
    private StepStatus appAccessRevocation;
    private int guestPassesRevoked;
    private LocalDateTime accessRevokedAt;

    // Step 3: RFID tag deactivation
    @Enumerated(EnumType.STRING)
    private StepStatus rfidDeactivation;
    private int tagsDeactivated;
    private int totalTags;
    private LocalDateTime rfidDeactivatedAt;

    // Step 4: Daily help unlinking
    @Enumerated(EnumType.STRING)
    private StepStatus dailyHelpUnlinking;
    private int helpsUnlinked;
    private LocalDateTime helpsUnlinkedAt;

    // Step 5: Gate pass for moving truck
    @Enumerated(EnumType.STRING)
    private StepStatus gatePassIssuance;
    private String gatePassNumber;
    private LocalDateTime gatePassIssuedAt;

    // Step 6: Audit log preservation
    @Enumerated(EnumType.STRING)
    private StepStatus auditPreservation;
    private int auditRecordsArchived;
    private LocalDateTime auditPreservedAt;

    // Compensation tracking
    private String failedStep;
    private String failureReason;
    private String compensationLog;
    private LocalDateTime completedAt;

    public enum SagaStatus {
        INITIATED, IN_PROGRESS, COMPLETED, FAILED, COMPENSATING, COMPENSATED
    }

    public enum StepStatus {
        PENDING, IN_PROGRESS, COMPLETED, FAILED, COMPENSATED, SKIPPED
    }
}
