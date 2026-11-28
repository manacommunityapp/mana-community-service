package com.manacommunity.api.guardpayroll.entity;

import com.manacommunity.api.model.common.BaseAuditEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "attendance_payroll_tests")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AttendancePayrollTest extends BaseAuditEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String communityId;
    private String testCaseCode;
    private String testCaseName;

    @Enumerated(EnumType.STRING)
    private PayrollScenario scenario;

    private String guardName;
    private String agencyName;
    private int totalShifts;
    private int presentDays;
    private int absentDays;
    private double overtimeHours;
    private double basicSalary;
    private double overtimePay;
    private double deductions;
    private double netPay;
    private boolean esicApplied;
    private boolean pfDeducted;
    private double esicAmount;
    private double pfAmount;
    private boolean payslipGenerated;
    private String paymentMode;

    @Enumerated(EnumType.STRING)
    private TestResult testResult;
    private String failureReason;

    public enum PayrollScenario {
        MONTHLY_SALARY_CORRECT, OVERTIME_CALCULATION_DOUBLE_RATE,
        ESIC_PF_DEDUCTION_CHECK, ABSENT_DAYS_SALARY_DEDUCTED,
        AGENCY_COMMISSION_SPLIT, BONUS_FESTIVAL_CALCULATION,
        MULTI_SOCIETY_GUARD_PAYROLL, PAYSLIP_PDF_GENERATION
    }

    public enum TestResult { PASS, FAIL, SKIPPED, PENDING }
}
