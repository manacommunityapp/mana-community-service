package com.manacommunity.api.finance.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "brs_session", indexes = {
        @Index(name = "idx_brs_session_community", columnList = "community_id")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BrsSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "community_id", nullable = false)
    private Long communityId;

    @Column(name = "bank_account_name", nullable = false, length = 120)
    private String bankAccountName;

    @Column(name = "statement_month", nullable = false, length = 10) // e.g. "2026-10"
    private String statementMonth;

    @Column(name = "statement_from_date")
    private LocalDate statementFromDate;

    @Column(name = "statement_to_date")
    private LocalDate statementToDate;

    @Column(nullable = false, length = 20)
    @Builder.Default
    private String status = "OPEN"; // OPEN, LOCKED

    @Column(name = "uploaded_at")
    private LocalDateTime uploadedAt;

    @Column(name = "locked_at")
    private LocalDateTime lockedAt;

    @Column(name = "opening_balance", precision = 16, scale = 2)
    @Builder.Default
    private BigDecimal openingBalance = BigDecimal.ZERO;

    @Column(name = "closing_balance", precision = 16, scale = 2)
    @Builder.Default
    private BigDecimal closingBalance = BigDecimal.ZERO;

    @OneToMany(mappedBy = "session", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<BrsLine> lines = new ArrayList<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        uploadedAt = LocalDateTime.now();
    }
}
