package com.manacommunity.api.cfbos.treasury.entity;

import com.manacommunity.api.cfbos.treasury.enums.ReconciliationStatus;
import com.manacommunity.api.cfbos.treasury.enums.StatementType;
import com.manacommunity.api.model.Community;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "cfbos_bank_transactions")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BankStatementTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "community_id", nullable = false)
    private Community community;

    private String statementReference;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatementType statementType;

    @Column(nullable = false)
    private LocalDate transactionDate;

    private LocalDate valueDate;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false)
    private String entryType; // CR or DR

    private String referenceNumber; // UTR or Cheque no.
    
    @Column(columnDefinition = "TEXT")
    private String narration;

    private String detectedFlatNumber;
    private String detectedPayerName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private ReconciliationStatus status = ReconciliationStatus.UNMATCHED;

    private Long matchedInvoiceId;
    private Long matchedPaymentId;
    private Double confidenceScore;

    @Column(nullable = false)
    @Builder.Default
    private LocalDateTime uploadedAt = LocalDateTime.now();
}
