package com.manacommunity.api.finance.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "society_vendor_bill", indexes = {
        @Index(name = "idx_svb_community", columnList = "community_id"),
        @Index(name = "idx_svb_status", columnList = "status")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SocietyVendorBill {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "community_id", nullable = false)
    private Long communityId;

    @Column(name = "bill_number", length = 40)
    private String billNumber;

    @Column(name = "vendor_id", length = 40)
    private String vendorId;

    @Column(name = "vendor_name", length = 120)
    private String vendorName;

    @Column(name = "bill_date")
    private LocalDate billDate;

    @Column(name = "due_date")
    private LocalDate dueDate;

    @Column(columnDefinition = "text")
    private String description;

    @Column(name = "gross_amount", nullable = false, precision = 14, scale = 2)
    private BigDecimal grossAmount;

    @Column(name = "gst_percent", precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal gstPercent = BigDecimal.ZERO;

    @Column(name = "gst_amount", precision = 14, scale = 2)
    @Builder.Default
    private BigDecimal gstAmount = BigDecimal.ZERO;

    @Column(name = "tds_section", length = 20)
    private String tdsSection;

    @Column(name = "tds_amount", precision = 14, scale = 2)
    @Builder.Default
    private BigDecimal tdsAmount = BigDecimal.ZERO;

    @Column(name = "net_payable", nullable = false, precision = 14, scale = 2)
    private BigDecimal netPayable;

    @Column(name = "paid_amount", precision = 14, scale = 2)
    @Builder.Default
    private BigDecimal paidAmount = BigDecimal.ZERO;

    @Column(name = "balance_due", precision = 14, scale = 2)
    private BigDecimal balanceDue;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private BillStatus status = BillStatus.RECEIVED;

    @Column(name = "approved_by_name", length = 100)
    private String approvedByName;

    @Column(name = "approved_date")
    private LocalDate approvedDate;

    @Column(name = "approval_notes", columnDefinition = "text")
    private String approvalNotes;

    @Column(name = "payment_method", length = 40)
    private String paymentMethod;

    @Column(name = "utr_reference", length = 60)
    private String utrReference;

    @Column(name = "paid_date")
    private LocalDate paidDate;

    @Column(name = "gl_voucher_number", length = 30)
    private String glVoucherNumber;

    @Column(name = "created_by_name", length = 100)
    private String createdByName;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public enum BillStatus {
        DRAFT, RECEIVED, APPROVED, PARTIALLY_PAID, PAID, OVERDUE, DISPUTED
    }

    @PrePersist
    void onCreate() {
        createdAt = updatedAt = LocalDateTime.now();
        if (status == null) status = BillStatus.RECEIVED;
        if (paidAmount == null) paidAmount = BigDecimal.ZERO;
        if (gstAmount == null) gstAmount = BigDecimal.ZERO;
        if (tdsAmount == null) tdsAmount = BigDecimal.ZERO;
        if (gstPercent == null) gstPercent = BigDecimal.ZERO;
        if (balanceDue == null) balanceDue = netPayable;
    }

    @PreUpdate
    void onUpdate() { updatedAt = LocalDateTime.now(); }
}
