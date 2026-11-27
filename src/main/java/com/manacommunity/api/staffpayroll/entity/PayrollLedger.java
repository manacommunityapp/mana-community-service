package com.manacommunity.api.staffpayroll.entity;

import com.manacommunity.api.model.common.BaseAuditEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "payroll_ledgers")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class PayrollLedger extends BaseAuditEntity {

    public enum PayrollStatus { DRAFT, CALCULATED, APPROVED, PAID, DISPUTED }

    @Column(nullable = false) private Long communityId;
    @Column(nullable = false) private Long staffId;
    @Column(nullable = false) private String staffName;
    @Column(nullable = false) private Long contractId;

    @Column(nullable = false) private String employerFlatNumber;
    @Column(nullable = false) private String employerName;

    private String payrollMonth;
    private LocalDate cycleStartDate;
    private LocalDate cycleEndDate;

    private int totalDaysInCycle;
    private int daysPresent;
    private int daysAbsent;
    private int halfDays;
    private int paidLeaves;
    private int weeklyOffs;
    private int unpaidLeaves;

    private double grossSalary;
    private double attendanceDeduction;
    private double advanceDeduction;
    private double bonusAmount;
    private double netPayable;

    @Enumerated(EnumType.STRING) @Column(nullable = false) private PayrollStatus status;

    private String paymentMode;
    private String paymentReference;
    private String receiptUrl;
    @Builder.Default private boolean receiptGenerated = false;
}
