package com.manacommunity.api.erpaudit.entity;

import com.manacommunity.api.model.common.BaseAuditEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "erp_maker_checker_tests")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class MakerCheckerTest extends BaseAuditEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String communityId;
    private String testCaseCode;
    private String testCaseName;

    @Enumerated(EnumType.STRING)
    private ReconciliationScenario scenario;

    private String expenseDescription;
    private BigDecimal expenseAmount;
    private String makerRole;
    private String makerName;
    private String checkerRole;
    private String checkerName;
    private String finalApproverRole;
    private String finalApproverName;

    @Enumerated(EnumType.STRING)
    private ApprovalChain approvalChain;

    private boolean makerApproved;
    private boolean checkerApproved;
    private boolean finalApproved;
    private boolean autoEscalated;
    private int escalationHours;

    private String tallyLedgerName;
    private String tallyVoucherType;
    private BigDecimal debitTotal;
    private BigDecimal creditTotal;
    private BigDecimal balanceDifference;
    private boolean doubleEntryValid;
    private String exportFormat;

    @Enumerated(EnumType.STRING)
    private InvoiceTestCase.TestResult testResult;

    private String failureReason;

    public enum ReconciliationScenario {
        THREE_LEVEL_APPROVAL, BYPASS_ATTEMPT, AUTO_ESCALATION_TIMEOUT,
        TALLY_XML_EXPORT, TALLY_CSV_EXPORT, DEBIT_CREDIT_MISMATCH,
        PARTIAL_APPROVAL_REJECT, CONCURRENT_APPROVAL_RACE
    }

    public enum ApprovalChain {
        ESTATE_MANAGER_TREASURER_SECRETARY,
        TREASURER_SECRETARY,
        ESTATE_MANAGER_ONLY,
        AUTO_APPROVE_BELOW_THRESHOLD
    }
}
