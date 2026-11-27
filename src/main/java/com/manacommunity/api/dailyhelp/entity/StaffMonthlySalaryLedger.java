package com.manacommunity.api.dailyhelp.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "staff_monthly_salary_ledger", schema = "manacommunity",
        uniqueConstraints = @UniqueConstraint(name = "uq_staff_monthly_salary", columnNames = {"contract_id", "billing_month"}))
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StaffMonthlySalaryLedger {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "contract_id", nullable = false)
    private FlatStaffContract contract;

    @Column(name = "billing_month", nullable = false, length = 7) // 'YYYY-MM'
    private String billingMonth;

    @Column(name = "calendar_days", nullable = false)
    private Integer calendarDays;

    @Column(name = "scheduled_working_days", nullable = false)
    private Integer scheduledWorkingDays;

    @Column(name = "present_days", nullable = false)
    private Integer presentDays;

    @Column(name = "paid_leave_days", nullable = false)
    private Integer paidLeaveDays;

    @Column(name = "unpaid_absence_days", nullable = false)
    private Integer unpaidAbsenceDays;

    @Column(name = "base_stipend", nullable = false, precision = 10, scale = 2)
    private BigDecimal baseStipend;

    @Builder.Default
    @Column(name = "pro_rata_deduction", nullable = false, precision = 10, scale = 2)
    private BigDecimal proRataDeduction = BigDecimal.ZERO;

    @Column(name = "net_payable", nullable = false, precision = 10, scale = 2)
    private BigDecimal netPayable;

    @Builder.Default
    @Column(name = "payment_status", nullable = false, length = 20)
    private String paymentStatus = "UNPAID";

    @Builder.Default
    @Column(name = "calculated_at", nullable = false)
    private LocalDateTime calculatedAt = LocalDateTime.now();
}
