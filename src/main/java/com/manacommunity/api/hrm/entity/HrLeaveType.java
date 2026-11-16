package com.manacommunity.api.hrm.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "hr_leave_types")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HrLeaveType {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id", nullable = false)
    private HrOrganization organization;

    @Column(name = "leave_type_name", nullable = false, length = 50)
    private String leaveTypeName;

    @Column(name = "default_balance", nullable = false)
    private Integer defaultBalance;

    @Column(name = "is_paid", nullable = false)
    @Builder.Default
    private Boolean isPaid = true;

    @Column(name = "is_carry_forward", nullable = false)
    @Builder.Default
    private Boolean isCarryForward = false;

    @Column(name = "max_carry_forward")
    private Integer maxCarryForward;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    // Multiple approval levels
    @Column(name = "approval_levels", nullable = false)
    @Builder.Default
    private Integer approvalLevels = 1;

    @Column(name = "requires_hr_approval", nullable = false)
    @Builder.Default
    private Boolean requiresHrApproval = false;

    // Policy-based approval fields
    @Column(name = "min_notice_days", nullable = false)
    @Builder.Default
    private Integer minNoticeDays = 0;

    @Column(name = "max_consecutive_days")
    private Integer maxConsecutiveDays;

    @Column(name = "max_per_year")
    private Integer maxPerYear;

    @Column(name = "allow_negative_balance", nullable = false)
    @Builder.Default
    private Boolean allowNegativeBalance = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
