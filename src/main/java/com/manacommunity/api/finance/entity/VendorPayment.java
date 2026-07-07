package com.manacommunity.api.finance.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * A money-paid record for the Expenses module — the outflow counterpart of
 * {@link FinanceReceipt}. Records a payment made to a vendor (against purchases,
 * purchase orders or as an on-account advance).
 */
@Entity
@Table(name = "finance_vendor_payments")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VendorPayment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Human-readable code, e.g. VPAY/001. */
    @Column(length = 24)
    private String code;

    /** Which Payment Vouchers tab this belongs to. */
    @Enumerated(EnumType.STRING)
    @Column(name = "payment_type", nullable = false, length = 16)
    @Builder.Default
    private PaymentType paymentType = PaymentType.PAID_BILL;

    @Column(name = "vendor_id")
    private Long vendorId;

    /** Denormalised vendor name for list display without a join. */
    @Column(name = "vendor_name", length = 160)
    private String vendorName;

    @Column(name = "payment_date", nullable = false)
    private LocalDate paymentDate;

    @Column(precision = 14, scale = 2)
    private BigDecimal amount;

    @Column(name = "payment_mode", length = 40)
    private String paymentMode;

    /** Bank / cash account the money was paid from. */
    @Column(name = "paid_from", length = 120)
    private String paidFrom;

    @Column(length = 120)
    private String reference;

    /** Free-form status, e.g. Paid / Unallocated / Allocated. */
    @Column(length = 30)
    private String status;

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

    /** Paid Bills | GST Advance Payments | Other Payments. */
    public enum PaymentType { PAID_BILL, ADVANCE, OTHER }
}
