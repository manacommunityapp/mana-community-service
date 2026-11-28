package com.manacommunity.api.dailysubscription.entity;

import com.manacommunity.api.model.common.BaseAuditEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "cutoff_vacation_tests")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CutoffVacationTest extends BaseAuditEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String communityId;
    private String testCaseCode;
    private String testCaseName;

    @Enumerated(EnumType.STRING)
    private CutoffScenario scenario;

    private String subscriptionType;
    private String vendorName;
    private String cutoffTime;
    private String orderModificationTime;
    private boolean beforeCutoff;
    private boolean modificationAccepted;

    private boolean vacationModeEnabled;
    private String vacationStartDate;
    private String vacationEndDate;
    private int skippedDays;
    private boolean skippedDaysBilled;

    private boolean billingCorrect;
    private double expectedAmount;
    private double actualAmount;
    private double varianceAmount;

    @Enumerated(EnumType.STRING)
    private TestResult testResult;
    private String failureReason;

    public enum CutoffScenario {
        ORDER_BEFORE_CUTOFF, ORDER_AFTER_CUTOFF, VACATION_PAUSE_RESUME,
        VACATION_PARTIAL_MONTH, SKIPPED_DAY_BILLING_CHECK, CUTOFF_BOUNDARY_11_59_59,
        CUTOFF_BOUNDARY_12_00_01, MODIFICATION_DURING_DISPATCH
    }

    public enum TestResult { PASS, FAIL, SKIPPED, PENDING }
}
