package com.manacommunity.api.finance.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "cheque_payment_record", indexes = {
        @Index(name = "idx_cheque_community", columnList = "community_id"),
        @Index(name = "idx_cheque_status", columnList = "status")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChequePaymentRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "community_id", nullable = false)
    private Long communityId;

    @Column(name = "resident_user_id")
    private Long residentUserId;

    @Column(name = "flat_no", length = 30)
    private String flatNo;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal amount;

    @Column(name = "cheque_number", nullable = false, length = 30)
    private String chequeNumber;

    @Column(name = "bank_name", length = 100)
    private String bankName;

    @Column(name = "cheque_date")
    private LocalDate chequeDate;

    @Column(name = "deposited_date")
    private LocalDate depositedDate;

    @Column(name = "cleared_date")
    private LocalDate clearedDate;

    @Column(nullable = false, length = 20)
    @Builder.Default
    private String status = "IN_HAND"; // IN_HAND, DEPOSITED, CLEARED, BOUNCED

    @Column(name = "invoice_id")
    private Long invoiceId;

    @Column(columnDefinition = "text")
    private String remarks;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
