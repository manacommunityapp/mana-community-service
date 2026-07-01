package com.manacommunity.api.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "invoice_line_items")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InvoiceLineItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "invoice_id", nullable = false)
    @JsonIgnore
    private VendorInvoice invoice;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "item_id")
    private InventoryItem item;

    @Column(nullable = false, length = 255)
    private String description;

    @Column(precision = 12, scale = 2)
    private BigDecimal quantity;

    @Column(name = "unit_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal unitPrice;

    @Column(name = "total_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalPrice;

    @Column(precision = 12, scale = 2)
    private BigDecimal cgst;

    @Column(precision = 12, scale = 2)
    private BigDecimal sgst;

    @Column(precision = 12, scale = 2)
    private BigDecimal igst;

    @Enumerated(EnumType.STRING)
    @Column(name = "expense_category", nullable = false, length = 40)
    private ExpenseCategory expenseCategory;

    @PrePersist
    protected void onCreate() {
        if (quantity == null) {
            quantity = BigDecimal.ONE;
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
}
