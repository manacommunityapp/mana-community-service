package com.manacommunity.api.homeservices.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "settlement_ledger_entries")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SettlementLedgerEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "settlement_id", nullable = false)
    private Long settlementId;

    @Column(name = "account_code", nullable = false, length = 64)
    private String accountCode;

    @Column(name = "entry_type", nullable = false, length = 16)
    private String entryType; // DEBIT, CREDIT

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(length = 255)
    private String description;

    @Builder.Default
    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();
}
