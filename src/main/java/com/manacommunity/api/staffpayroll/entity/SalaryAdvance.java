package com.manacommunity.api.staffpayroll.entity;

import com.manacommunity.api.model.common.BaseAuditEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "salary_advances")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class SalaryAdvance extends BaseAuditEntity {

    public enum AdvanceStatus { ACTIVE, FULLY_REPAID, WRITTEN_OFF, TERMINATED_PENDING }
    public enum PaymentMethod { UPI, CASH, BANK_TRANSFER, IN_APP_WALLET }

    @Column(nullable = false) private Long communityId;
    @Column(nullable = false) private Long staffId;
    @Column(nullable = false) private String staffName;
    @Column(nullable = false) private Long contractId;

    @Column(nullable = false) private String employerFlatNumber;
    @Column(nullable = false) private String employerName;

    private double principalAmount;
    private int totalInstallments;
    private double emiAmount;
    private int installmentsPaid;
    private double amountRepaid;
    private double outstandingBalance;

    @Enumerated(EnumType.STRING) @Column(nullable = false) private AdvanceStatus status;
    @Enumerated(EnumType.STRING) private PaymentMethod paymentMethod;

    private String upiTransactionId;
    private LocalDateTime disbursedAt;
    private LocalDate firstEmiDate;
    private LocalDate lastEmiDate;

    @Column(length = 500) private String purpose;
    @Column(length = 500) private String receiptUrlEn;
    @Column(length = 500) private String receiptUrlHi;

    @Builder.Default private boolean terminatedEarly = false;
    private double settlementAmount;
    private LocalDate terminationDate;
}
