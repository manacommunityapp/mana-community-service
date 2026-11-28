package com.manacommunity.api.builderhandover.entity;

import com.manacommunity.api.model.common.BaseAuditEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "corpus_fund_tests")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CorpusFundTest extends BaseAuditEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String communityId;
    private String testCaseCode;
    private String testCaseName;

    @Enumerated(EnumType.STRING)
    private CorpusScenario scenario;

    private double openingBalance;
    private double corpusFundAmount;
    private double gstInputCredit;
    private double escrowDisputedAmount;
    private boolean balanceLocked;
    private boolean gstCarryoverCorrect;
    private boolean escrowTracked;
    private int pendingVendorBills;
    private double pendingBillAmount;
    private boolean handoverSnapshotGenerated;
    private String handoverDate;
    private boolean rwaAccepted;

    @Enumerated(EnumType.STRING)
    private TestResult testResult;
    private String failureReason;

    public enum CorpusScenario {
        OPENING_BALANCE_LOCKED, GST_INPUT_CREDIT_CARRYOVER,
        ESCROW_DISPUTED_VENDOR_BILLS, HANDOVER_SNAPSHOT_GENERATION,
        RWA_ACCEPTANCE_SIGNATURE, BALANCE_MISMATCH_BUILDER_VS_RWA,
        PARTIAL_HANDOVER_PHASED, AUDIT_TRAIL_COMPLETE
    }

    public enum TestResult { PASS, FAIL, SKIPPED, PENDING }
}
