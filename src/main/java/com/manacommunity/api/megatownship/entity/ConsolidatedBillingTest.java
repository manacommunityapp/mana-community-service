package com.manacommunity.api.megatownship.entity;

import com.manacommunity.api.model.common.BaseAuditEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "consolidated_billing_tests")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ConsolidatedBillingTest extends BaseAuditEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String communityId;
    private String testCaseCode;
    private String testCaseName;

    @Enumerated(EnumType.STRING)
    private BillingScenario scenario;

    private String towerName;
    private String invoiceType;
    private double towerAmount;
    private double federationLevy;
    private double totalAmount;
    private boolean gstApplied;
    private double gstAmount;
    private boolean invoiceMerged;
    private boolean paymentSplitCorrect;
    private String paymentGateway;
    private boolean receiptGenerated;

    @Enumerated(EnumType.STRING)
    private TestResult testResult;
    private String failureReason;

    public enum BillingScenario {
        MULTI_TOWER_CONSOLIDATED_INVOICE, GST_SPLIT_ACROSS_ENTITIES,
        FEDERATION_LEVY_CALCULATION, PARTIAL_PAYMENT_ALLOCATION,
        REFUND_TO_CORRECT_TOWER_WALLET, OVERDUE_INTEREST_FEDERATION_SHARE,
        ADVANCE_PAYMENT_MULTI_TOWER, RECEIPT_MERGE_PDF_GENERATION
    }

    public enum TestResult { PASS, FAIL, SKIPPED, PENDING }
}
