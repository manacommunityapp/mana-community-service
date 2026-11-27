package com.manacommunity.api.dailyhelp.entity;

import com.manacommunity.api.model.Community;
import com.manacommunity.api.user.model.AppUser;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Entity
@Table(name = "flat_staff_contract", schema = "manacommunity",
        uniqueConstraints = @UniqueConstraint(name = "uq_flat_staff_contract", columnNames = {"flat_id", "affiliation_id", "service_type"}))
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FlatStaffContract {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "affiliation_id", nullable = false)
    private SocietyStaffAffiliation affiliation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "community_id", nullable = false)
    private Community community;

    @Column(name = "flat_id", nullable = false)
    private Long flatId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "resident_id", nullable = false)
    private AppUser resident;

    @Column(name = "service_type", nullable = false, length = 30)
    private String serviceType;

    @Builder.Default
    @Column(name = "days_of_week_mask", nullable = false)
    private Integer daysOfWeekMask = 127; // 1=Mon, 2=Tue, 4=Wed, 8=Thu, 16=Fri, 32=Sat, 64=Sun (127=all days)

    @Column(name = "scheduled_start_time", nullable = false)
    private LocalTime scheduledStartTime;

    @Column(name = "scheduled_end_time", nullable = false)
    private LocalTime scheduledEndTime;

    @Column(name = "monthly_stipend", nullable = false, precision = 10, scale = 2)
    private BigDecimal monthlyStipend;

    @Builder.Default
    @Column(name = "leave_allowance_per_month", nullable = false)
    private Integer leaveAllowancePerMonth = 2;

    @Builder.Default
    @Column(name = "deduction_policy", nullable = false, length = 30)
    private String deductionPolicy = "WORKING_DAYS_PRO_RATA";

    @Builder.Default
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Builder.Default
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}
