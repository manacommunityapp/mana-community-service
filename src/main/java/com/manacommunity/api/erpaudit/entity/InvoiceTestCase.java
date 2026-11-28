package com.manacommunity.api.erpaudit.entity;

import com.manacommunity.api.model.common.BaseAuditEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "erp_invoice_test_cases")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class InvoiceTestCase extends BaseAuditEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String communityId;
    private String testCaseCode;
    private String testCaseName;

    @Enumerated(EnumType.STRING)
    private InvoiceScenario scenario;

    private String flatNumber;
    private LocalDate moveInDate;
    private LocalDate billingPeriodStart;
    private LocalDate billingPeriodEnd;
    private int totalDaysInMonth;
    private int occupiedDays;

    private BigDecimal fullMonthAmount;
    private BigDecimal proRataAmount;
    private BigDecimal gstRate;
    private BigDecimal gstAmount;
    private boolean gstExempt;
    private String gstExemptionReason;
    private BigDecimal lateFeeRate;
    private BigDecimal lateFeeAmount;
    private boolean compoundInterest;
    private int overdueDays;

    private BigDecimal totalExpected;
    private BigDecimal totalActual;
    private BigDecimal variance;

    private boolean manualWaiverApplied;
    private String waiverApprovedBy;
    private String waiverReason;

    @Enumerated(EnumType.STRING)
    private TestResult testResult;

    private String failureReason;

    public enum InvoiceScenario {
        MID_MONTH_MOVE_IN, GST_BELOW_THRESHOLD, GST_ABOVE_THRESHOLD,
        COMPOUND_LATE_INTEREST, SIMPLE_LATE_INTEREST, MANUAL_WAIVER,
        PARTIAL_PAYMENT_CARRYOVER, ADVANCE_WALLET_ADJUSTMENT
    }

    public enum TestResult { PASS, FAIL, SKIPPED, PENDING }
}
