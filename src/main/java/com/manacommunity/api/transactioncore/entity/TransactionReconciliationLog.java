package com.manacommunity.api.transactioncore.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "transaction_reconciliation_log", indexes = {
        @Index(name = "idx_recon_date", columnList = "recon_date")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionReconciliationLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "recon_id", nullable = false, unique = true, length = 64)
    private String reconId;

    @Column(name = "recon_date", nullable = false)
    private LocalDate reconDate;

    @Column(name = "community_id")
    private Long communityId;

    @Column(name = "total_transactions")
    @Builder.Default
    private Integer totalTransactions = 0;

    @Column(name = "matched_count")
    @Builder.Default
    private Integer matchedCount = 0;

    @Column(name = "mismatched_count")
    @Builder.Default
    private Integer mismatchedCount = 0;

    @Column(name = "auto_resolved_count")
    @Builder.Default
    private Integer autoResolvedCount = 0;

    @Column(nullable = false, length = 32)
    @Builder.Default
    private String status = "COMPLETED";

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "discrepancies_json", columnDefinition = "jsonb")
    private String discrepanciesJson;

    @Column(name = "executed_at", nullable = false)
    @Builder.Default
    private LocalDateTime executedAt = LocalDateTime.now();
}
