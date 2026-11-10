package com.manacommunity.api.hrm.entity;

import com.manacommunity.api.hrm.enums.LeaveApprovalStatus;
import com.manacommunity.api.hrm.enums.LeaveSession;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "hr_leave_requests")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HrLeaveRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "organization_id", nullable = false)
    private Long organizationId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id", nullable = false)
    private HrEmployee employee;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "leave_type_id", nullable = false)
    private HrLeaveType leaveType;

    @Column(name = "from_date", nullable = false)
    private LocalDate fromDate;

    @Column(name = "to_date", nullable = false)
    private LocalDate toDate;

    @Column(name = "total_days", nullable = false)
    private Double totalDays;

    @Column(name = "reason", columnDefinition = "TEXT")
    private String reason;

    @Enumerated(EnumType.STRING)
    @Column(name = "approval_status", nullable = false, length = 30)
    @Builder.Default
    private LeaveApprovalStatus approvalStatus = LeaveApprovalStatus.PENDING;

    // Half-day leave support
    @Column(name = "is_half_day", nullable = false)
    @Builder.Default
    private Boolean isHalfDay = false;

    @Enumerated(EnumType.STRING)
    @Column(name = "leave_session", length = 20)
    private LeaveSession leaveSession;

    // Multi-level approval tracking
    @Column(name = "approval_level", nullable = false)
    @Builder.Default
    private Integer approvalLevel = 1;

    @Column(name = "l1_approved_by")
    private Long l1ApprovedBy;

    @Column(name = "l1_approved_at")
    private LocalDateTime l1ApprovedAt;

    @Column(name = "l1_remarks", length = 500)
    private String l1Remarks;

    @Column(name = "approved_by")
    private Long approvedBy;

    @Column(name = "approver_remarks")
    private String approverRemarks;

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
