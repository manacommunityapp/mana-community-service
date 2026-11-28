package com.manacommunity.api.procurement.entity;

import com.manacommunity.api.model.common.BaseAuditEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "proc_approval_workflow")
@Getter @Setter @NoArgsConstructor
public class ProcApprovalWorkflow extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long communityId;

    @Column(nullable = false)
    private Long invoiceId;

    private String poNumber;
    private String invoiceNumber;

    @Column(nullable = false)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ApprovalLevel currentLevel;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private WorkflowStatus status = WorkflowStatus.PENDING;

    private String estateManagerId;
    private LocalDateTime estateManagerApprovedAt;
    private String estateManagerRemarks;

    private String treasurerId;
    private LocalDateTime treasurerApprovedAt;
    private String treasurerRemarks;

    private String secretaryId;
    private LocalDateTime secretaryApprovedAt;
    private String secretaryRemarks;

    private String rejectedBy;
    private String rejectionReason;
    private LocalDateTime rejectedAt;

    public enum ApprovalLevel {
        ESTATE_MANAGER, TREASURER, SECRETARY
    }

    public enum WorkflowStatus {
        PENDING, ESTATE_MANAGER_APPROVED, TREASURER_APPROVED, FULLY_APPROVED, REJECTED
    }
}
