package com.manacommunity.api.staffpayroll.entity;

import com.manacommunity.api.model.common.BaseAuditEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "advance_installments")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class AdvanceInstallment extends BaseAuditEntity {

    public enum InstallmentStatus { SCHEDULED, DEDUCTED, SKIPPED, WAIVED }

    @Column(nullable = false) private Long advanceId;
    @Column(nullable = false) private Long staffId;
    @Column(nullable = false) private Long contractId;

    private int installmentNumber;
    private double amount;
    private LocalDate dueDate;
    private String payrollMonth;

    @Enumerated(EnumType.STRING) @Column(nullable = false) private InstallmentStatus status;

    private LocalDateTime deductedAt;
    private double balanceAfterDeduction;
    @Column(length = 300) private String remarks;
}
