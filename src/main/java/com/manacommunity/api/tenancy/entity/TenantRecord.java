package com.manacommunity.api.tenancy.entity;

import com.manacommunity.api.model.common.BaseAuditEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "tenant_records")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class TenantRecord extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long communityId;
    private String flatNumber;
    private String towerName;
    private String tenantName;
    private String tenantPhone;
    private String tenantEmail;
    private Long ownerId;

    @Enumerated(EnumType.STRING)
    private TenantStatus status;

    private LocalDate moveInDate;
    private LocalDate moveOutDate;
    private LocalDate leaseStartDate;
    private LocalDate leaseEndDate;

    private boolean appAccessActive;
    private int activeRfidTags;
    private int linkedDailyHelps;
    private double pendingDues;

    @Enumerated(EnumType.STRING)
    private KycStatus kycStatus;

    private String moveInSlotId;
    private String moveOutSlotId;

    public enum TenantStatus {
        KYC_PENDING, KYC_IN_REVIEW, ACTIVE, MOVE_OUT_INITIATED,
        MOVE_OUT_IN_PROGRESS, MOVED_OUT, REJECTED
    }

    public enum KycStatus {
        NOT_STARTED, DOCUMENTS_UPLOADED, OCR_PROCESSING, VERIFICATION_PENDING,
        MC_REVIEW, APPROVED, REJECTED, EXPIRED
    }
}
