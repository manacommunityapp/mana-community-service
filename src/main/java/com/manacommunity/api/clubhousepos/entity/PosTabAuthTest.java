package com.manacommunity.api.clubhousepos.entity;

import com.manacommunity.api.model.common.BaseAuditEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "pos_tab_auth_tests")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class PosTabAuthTest extends BaseAuditEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String communityId;
    private String testCaseCode;
    private String testCaseName;

    @Enumerated(EnumType.STRING)
    private PosScenario scenario;

    private String flatNumber;
    private String orderDescription;
    private double orderAmount;
    private double flatCreditLimit;
    private double currentTabBalance;
    private boolean creditLimitExceeded;
    private String authMethod;
    private boolean pinVerified;
    private boolean pushApprovalSent;
    private boolean pushApprovalReceived;
    private boolean batchReconciled;
    private double batchTotal;
    private double ledgerTotal;
    private boolean ledgerMatchesBatch;

    @Enumerated(EnumType.STRING)
    private TestResult testResult;
    private String failureReason;

    public enum PosScenario {
        FLAT_TAB_ORDER_WITHIN_LIMIT, CREDIT_LIMIT_EXCEEDED_BLOCKED,
        PIN_AUTH_WRONG_3_ATTEMPTS, PUSH_APPROVAL_TIMEOUT_60S,
        UNAUTHORIZED_FLAT_TAB_CHARGE, DAILY_BATCH_RECONCILIATION,
        BATCH_LEDGER_MISMATCH, CANCELLED_ORDER_REVERSAL
    }

    public enum TestResult { PASS, FAIL, SKIPPED, PENDING }
}
