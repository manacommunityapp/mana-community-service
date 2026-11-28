package com.manacommunity.api.erpaudit.entity;

import com.manacommunity.api.model.common.BaseAuditEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "erp_payment_webhook_tests")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class PaymentWebhookTest extends BaseAuditEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String communityId;
    private String testCaseCode;
    private String testCaseName;

    @Enumerated(EnumType.STRING)
    private WebhookScenario scenario;

    private String transactionId;
    private String gatewayName;
    private BigDecimal amount;
    private String webhookStatus;
    private LocalDateTime paymentInitiatedAt;
    private LocalDateTime webhookReceivedAt;
    private long webhookDelaySeconds;
    private int duplicateCallbackCount;
    private boolean idempotencyHandled;
    private boolean walletAutoAdjusted;
    private BigDecimal walletBalanceBefore;
    private BigDecimal walletBalanceAfter;

    @Enumerated(EnumType.STRING)
    private BounceType bounceType;

    private String bounceReason;
    private boolean autoReversalTriggered;

    @Enumerated(EnumType.STRING)
    private InvoiceTestCase.TestResult testResult;

    private String failureReason;

    public enum WebhookScenario {
        DELAYED_WEBHOOK_30MIN, DUPLICATE_SUCCESS_CALLBACK, PARTIAL_PAYMENT,
        BOUNCED_CHEQUE, ADVANCE_WALLET_DEBIT, GATEWAY_TIMEOUT_RETRY,
        CONCURRENT_DOUBLE_PAYMENT, REFUND_WEBHOOK_MISMATCH
    }

    public enum BounceType { NONE, INSUFFICIENT_FUNDS, SIGNATURE_MISMATCH, ACCOUNT_CLOSED, TECHNICAL_DECLINE }
}
