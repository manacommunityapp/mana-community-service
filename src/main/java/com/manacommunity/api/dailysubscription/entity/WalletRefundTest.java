package com.manacommunity.api.dailysubscription.entity;

import com.manacommunity.api.model.common.BaseAuditEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "wallet_refund_tests")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class WalletRefundTest extends BaseAuditEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String communityId;
    private String testCaseCode;
    private String testCaseName;

    @Enumerated(EnumType.STRING)
    private WalletScenario scenario;

    private double walletBalanceBefore;
    private double walletBalanceAfter;
    private double orderAmount;
    private double refundAmount;

    private String batchRunTime;
    private boolean lowBalanceAlertSent;
    private boolean subscriptionPaused;
    private boolean autoRefundTriggered;
    private int refundDelayMs;

    private String vendorAction;
    private String refundReason;

    @Enumerated(EnumType.STRING)
    private TestResult testResult;
    private String failureReason;

    public enum WalletScenario {
        LOW_BALANCE_BEFORE_BATCH, ZERO_BALANCE_AUTO_PAUSE,
        OUT_OF_STOCK_INSTANT_REFUND, UNDELIVERED_ITEM_REFUND,
        PARTIAL_DELIVERY_PARTIAL_REFUND, WALLET_OVERDRAFT_PREVENTION,
        CONCURRENT_DEBIT_RACE_CONDITION, REFUND_AFTER_MANUAL_CANCEL
    }

    public enum TestResult { PASS, FAIL, SKIPPED, PENDING }
}
