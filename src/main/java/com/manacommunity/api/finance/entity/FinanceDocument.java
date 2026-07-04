package com.manacommunity.api.finance.entity;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * A unified financial document. The {@link Type} discriminator makes one table
 * serve invoices, estimates, sales orders, credit notes and refunds — they share
 * the same customer + line-item + totals shape in the Income &amp; Expense UI.
 */
@Entity
@Table(name = "finance_documents")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FinanceDocument {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Human-readable code, e.g. INV/001, EST/001, SLSODR/001, CN/001. */
    @Column(length = 24)
    private String code;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private Type type;

    /** Free-form status; values differ by type (Paid/Unpaid, Approved, Open…). */
    @Column(length = 30)
    private String status;

    @Column(name = "customer_id")
    private Long customerId;

    /** Denormalised customer name for list display without a join. */
    @Column(name = "customer_name", length = 160)
    private String customerName;

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

    @JsonManagedReference
    @OneToMany(mappedBy = "document", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<FinanceDocumentLine> lines = new ArrayList<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    void onCreate() { createdAt = updatedAt = LocalDateTime.now(); }

    @PreUpdate
    void onUpdate() { updatedAt = LocalDateTime.now(); }

    public enum Type { INVOICE, ESTIMATE, SALES_ORDER, CREDIT_NOTE, REFUND, EXPENSE, PURCHASE, PURCHASE_RETURN, PURCHASE_ORDER, DEBIT_NOTE }
}
