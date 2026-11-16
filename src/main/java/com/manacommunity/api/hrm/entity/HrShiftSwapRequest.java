package com.manacommunity.api.hrm.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "hr_shift_swap_requests")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HrShiftSwapRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "organization_id", nullable = false)
    private Long organizationId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "requester_employee_id", nullable = false)
    private HrEmployee requesterEmployee;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "target_employee_id", nullable = false)
    private HrEmployee targetEmployee;

    @Column(name = "requester_shift_date", nullable = false)
    private LocalDate requesterShiftDate;

    @Column(name = "target_shift_date", nullable = false)
    private LocalDate targetShiftDate;

    @Column(name = "reason", length = 500)
    private String reason;

    // PENDING_COLLEAGUE → PENDING_MANAGER → APPROVED / REJECTED
    @Column(name = "status", nullable = false, length = 30)
    @Builder.Default
    private String status = "PENDING_COLLEAGUE";

    @Column(name = "colleague_responded_at")
    private LocalDateTime colleagueRespondedAt;

    @Column(name = "manager_responded_at")
    private LocalDateTime managerRespondedAt;

    @Column(name = "approved_by")
    private Long approvedBy;

    @Column(name = "rejection_reason", length = 500)
    private String rejectionReason;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
