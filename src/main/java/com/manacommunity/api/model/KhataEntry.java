package com.manacommunity.api.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "khata_entries")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KhataEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "party_id", nullable = false)
    private KhataParty party;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private TransactionKind kind;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal amount;

    @Column(length = 255)
    private String note;

    @Column(name = "entry_date", nullable = false)
    private LocalDate entryDate;

    @Column(name = "has_receipt", nullable = false)
    @Builder.Default
    private Boolean hasReceipt = false;

    @Column(name = "reminder_sent", nullable = false)
    @Builder.Default
    private Boolean reminderSent = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public enum TransactionKind {
        GAVE, GOT
    }

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (entryDate == null) {
            entryDate = LocalDate.now();
        }
    }
}
