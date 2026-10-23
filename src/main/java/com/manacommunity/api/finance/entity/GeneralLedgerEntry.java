package com.manacommunity.api.finance.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "general_ledger_entry", indexes = {
        @Index(name = "idx_gle_community", columnList = "community_id"),
        @Index(name = "idx_gle_entry_date", columnList = "entry_date")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GeneralLedgerEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "community_id", nullable = false)
    private Long communityId;

    @Column(name = "entry_date", nullable = false)
    private LocalDate entryDate;

    @Column(name = "voucher_number", length = 30)
    private String voucherNumber;

    @Column(name = "account_code", nullable = false, length = 10)
    private String accountCode;

    @Column(name = "account_name", length = 120)
    private String accountName;

    @Column(precision = 14, scale = 2)
    @Builder.Default
    private BigDecimal debit = BigDecimal.ZERO;

    @Column(precision = 14, scale = 2)
    @Builder.Default
    private BigDecimal credit = BigDecimal.ZERO;

    @Column(length = 250)
    private String description;

    @Column(name = "reference_type", length = 30)
    private String referenceType;

    @Column(name = "reference_id", length = 30)
    private String referenceId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() {
        createdAt = LocalDateTime.now();
        if (debit == null) debit = BigDecimal.ZERO;
        if (credit == null) credit = BigDecimal.ZERO;
    }
}
