package com.manacommunity.api.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Entity
@Table(name = "invoices")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VendorInvoice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "invoice_number", nullable = false, unique = true, length = 60)
    private String invoiceNumber;

    @Column(name = "invoice_date", nullable = false, length = 40)
    private String invoiceDate;

    @Column(name = "vendor_name", nullable = false, length = 120)
    private String vendorName;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vendor_id")
    private Vendor vendor;

    @Column(name = "vendor_gst_number", length = 30)
    private String vendorGstNumber;

    @Column(name = "total_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalAmount;

    @Column(name = "taxable_amount", precision = 12, scale = 2)
    private BigDecimal taxableAmount;

    @Column(precision = 12, scale = 2)
    private BigDecimal cgst;

    @Column(precision = 12, scale = 2)
    private BigDecimal sgst;

    @Column(precision = 12, scale = 2)
    private BigDecimal igst;

    @Enumerated(EnumType.STRING)
    @Column(name = "expense_category", length = 40)
    private ExpenseCategory expenseCategory;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private InvoiceStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_status", nullable = false, length = 30)
    private PaymentStatus paymentStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_mode", length = 30)
    private PaymentMode paymentMode;

    @Column(name = "payment_reference", length = 120)
    private String paymentReference;

    @Column(name = "due_date")
    private LocalDate dueDate;

    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    @Column(name = "purchase_request_id")
    private Long purchaseRequestId;

    @Column(name = "purchase_order_number", length = 80)
    private String purchaseOrderNumber;

    @Column(name = "uploaded_by", nullable = false, length = 120)
    private String uploadedBy;

    @Column(name = "receipt_url", nullable = false, length = 255)
    private String receiptUrl;

    @Column(name = "approval_notes", length = 255)
    private String approvalNotes;

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    @Column(name = "rejected_at")
    private LocalDateTime rejectedAt;

    @Column(name = "upi_id", length = 64)
    private String upiId;

    @Column(name = "bank_details", length = 255)
    private String bankDetails;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "ocr_raw_metadata", columnDefinition = "jsonb")
    private Map<String, Object> ocrRawMetadata;

    @OneToMany(mappedBy = "invoice", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<InvoiceLineItem> lineItems = new ArrayList<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (status == null) {
            status = InvoiceStatus.PENDING;
        }
        if (paymentStatus == null) {
            paymentStatus = PaymentStatus.PENDING;
        }
        if (taxableAmount == null) {
            taxableAmount = totalAmount;
        }
        if (cgst == null) {
            cgst = BigDecimal.ZERO;
        }
        if (sgst == null) {
            sgst = BigDecimal.ZERO;
        }
        if (igst == null) {
            igst = BigDecimal.ZERO;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
