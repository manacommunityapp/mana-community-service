package com.manacommunity.api.finance.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** Stock purchase / vendor bill (Expense → Stock Purchases menu). Standalone table. */
@Entity
@Table(name = "finance_purchase")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FinancePurchase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Human-readable code, e.g. PUR/001. */
    @Column(length = 24)
    private String code;

    /** Paid / Unpaid / Partial / Returned. */
    @Column(length = 30)
    private String status;

    @Column(name = "vendor_id")
    private Long vendorId;

    @Column(name = "vendor_name", length = 160)
    private String vendorName;

    @Column(name = "doc_date", nullable = false)
    private LocalDate docDate;

    @Column(name = "due_date")
    private LocalDate dueDate;

    @Column(columnDefinition = "text")
    private String notes;

    @Column(columnDefinition = "text")
    private String terms;

    @Column(name = "tax_inclusive", nullable = false)
    @Builder.Default
    private boolean taxInclusive = false;

    @Column(length = 8)
    private String currency;

    @Column(precision = 14, scale = 2) private BigDecimal subtotal;
    @Column(precision = 14, scale = 2) private BigDecimal discount;
    @Column(precision = 14, scale = 2) private BigDecimal tax;
    @Column(name = "other_charges", precision = 14, scale = 2) private BigDecimal otherCharges;
    @Column(name = "grand_total", precision = 14, scale = 2) private BigDecimal grandTotal;

    @ElementCollection
    @CollectionTable(name = "finance_purchase_line", joinColumns = @JoinColumn(name = "purchase_id"))
    @Builder.Default
    private List<FinanceLineItem> lines = new ArrayList<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    void onCreate() { createdAt = updatedAt = LocalDateTime.now(); }

    @PreUpdate
    void onUpdate() { updatedAt = LocalDateTime.now(); }
}
