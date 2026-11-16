package com.manacommunity.api.finance.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "brs_line", indexes = {
        @Index(name = "idx_brs_line_session", columnList = "session_id"),
        @Index(name = "idx_brs_line_match_status", columnList = "match_status")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BrsLine {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", nullable = false)
    private BrsSession session;

    @Column(name = "value_date", nullable = false)
    private LocalDate valueDate;

    @Column(nullable = false, length = 500)
    private String description;

    @Column(name = "credit_amount", precision = 16, scale = 2)
    private BigDecimal creditAmount;

    @Column(name = "debit_amount", precision = 16, scale = 2)
    private BigDecimal debitAmount;

    @Column(name = "utr_reference", length = 100)
    private String utrReference;

    @Column(name = "match_status", nullable = false, length = 30)
    @Builder.Default
    private String matchStatus = "UNMATCHED"; // UNMATCHED, MATCHED_INVOICE, MATCHED_PAYMENT, MATCHED_VENDOR, SUSPENSE

    @Column(name = "matched_entity_id")
    private Long matchedEntityId;

    @Column(name = "matched_entity_type", length = 40)
    private String matchedEntityType;

    @Column(name = "manual_note", length = 500)
    private String manualNote;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
