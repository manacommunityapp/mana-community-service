package com.manacommunity.api.finance.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * A money-received record. The {@link ReceiptType} distinguishes the three
 * Receipts tabs: invoice receipts, advance receipts and other income.
 */
@Entity
@Table(name = "finance_receipts")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FinanceReceipt {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Human-readable code, e.g. RCPT/001, ARCPT/001, OINC/001. */
    @Column(length = 24)
    private String code;

    @Enumerated(EnumType.STRING)
    @Column(name = "receipt_type", nullable = false, length = 12)
    private ReceiptType receiptType;

    @Column(name = "customer_id")
    private Long customerId;

    @Column(name = "customer_name", length = 160)
    private String customerName;

    @Column(name = "receipt_date", nullable = false)
    private LocalDate receiptDate;

    @Column(precision = 14, scale = 2)
    private BigDecimal amount;

    @Column(name = "payment_mode", length = 40)
    private String paymentMode;

    @Column(length = 120)
    private String reference;

    @Column(columnDefinition = "text")
    private String notes;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    void onCreate() { createdAt = updatedAt = LocalDateTime.now(); }

    @PreUpdate
    void onUpdate() { updatedAt = LocalDateTime.now(); }

    public enum ReceiptType { INVOICE, ADVANCE, OTHER }
}
