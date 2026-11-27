package com.manacommunity.api.handover.entity;

import com.manacommunity.api.model.common.BaseAuditEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "corpus_fund_transactions")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class CorpusFundTransaction extends BaseAuditEntity {

    public enum TransactionType {
        BUILDER_DEPOSIT, MAINTENANCE_EXPENSE, UTILITY_PAYMENT, STAFF_SALARY,
        REPAIR_FUND, SINKING_FUND, INSURANCE_PREMIUM, LEGAL_EXPENSE,
        OPENING_BALANCE, DISPUTED_CHARGE, ESCROW_HOLD, ESCROW_RELEASE,
        INTEREST_EARNED, REFUND, ADJUSTMENT
    }

    public enum TransactionStatus { VERIFIED, PENDING_VERIFICATION, DISPUTED, FROZEN, WRITTEN_OFF }

    public enum CutoverPhase { PRE_CUTOVER, SNAPSHOT_FROZEN, CUTOVER_IN_PROGRESS, POST_CUTOVER, RECONCILED }

    @Column(nullable = false) private Long communityId;
    @Column(nullable = false) private Long projectId;

    @Column(nullable = false, unique = true) private String voucherNumber;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private TransactionType type;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private TransactionStatus status;
    @Enumerated(EnumType.STRING) private CutoverPhase cutoverPhase;

    @Column(nullable = false) private double amount;
    private double gstAmount;
    private double tdsDeducted;
    private double netAmount;

    @Column(length = 500) private String description;
    private String category;
    private String subCategory;

    private LocalDate transactionDate;
    private String financialYear;
    private int monthNumber;

    private String paidToOrReceivedFrom;
    private String paymentMode;
    private String referenceNumber;
    private String bankAccountNumber;

    private String approvedByName;
    private String approvedByRole;
    private LocalDateTime approvedAt;

    private String verifiedByName;
    private LocalDateTime verifiedAt;

    @Column(length = 500) private String disputeReason;
    private String disputeRaisedBy;
    private LocalDateTime disputeRaisedAt;
    private LocalDateTime disputeResolvedAt;
    @Column(length = 500) private String disputeResolution;

    private String documentUrl;
    @Builder.Default private boolean partOfCutover = false;
    @Builder.Default private boolean frozen = false;
}
