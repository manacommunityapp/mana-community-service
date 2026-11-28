package com.manacommunity.api.handover.entity;

import com.manacommunity.api.model.common.BaseAuditEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "cutover_snapshots")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class CutoverSnapshot extends BaseAuditEntity {

    public enum CutoverStatus {
        DRAFT, BUILDER_SUBMITTED, RWA_REVIEW, DISPUTED, ARBITRATION,
        OPENING_BALANCE_FROZEN, CUTOVER_COMPLETE, RECONCILED
    }

    @Column(nullable = false) private Long communityId;
    @Column(nullable = false) private Long projectId;

    @Column(nullable = false) private String snapshotCode;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private CutoverStatus status;

    private LocalDate cutoverDate;
    private String financialYearFrom;
    private String financialYearTo;

    private double totalCorpusFund;
    private double maintenanceReserve;
    private double sinkingFund;
    private double repairFund;
    private double insuranceFund;
    private double totalEscrowHeld;

    private double totalExpenses18Months;
    private int totalVouchers;
    private int verifiedVouchers;
    private int disputedVouchers;
    private int pendingVouchers;

    private double openingBalanceRwa;
    private double gstInputCreditCarryover;
    @Builder.Default private boolean openingBalanceFrozen = false;
    private LocalDateTime frozenAt;
    private String frozenByName;

    private String builderSignatoryName;
    private String builderSignatoryDesignation;
    private LocalDateTime builderSignedAt;

    private String rwaPresidentName;
    private String rwaTreasurerName;
    private LocalDateTime rwaSignedAt;

    private String auditorName;
    private String auditorFirm;
    private LocalDateTime auditCompletedAt;
    @Column(length = 1000) private String auditRemarks;

    private int totalFlats;
    private int possessionCompleted;
    private int possessionPending;
    private double handoverCompletionPercent;
}
