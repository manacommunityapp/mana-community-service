package com.manacommunity.api.finance.entity;

import com.manacommunity.api.model.common.BaseAuditEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/** Customer sales order (Income → Sales Orders menu). Standalone table. */
@Entity
@Table(name = "finance_sales_order")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FinanceSalesOrder extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Human-readable code, e.g. SLSODR/001. */
    @Column(length = 24)
    private String code;

    /** Open / Confirmed / Fulfilled / Closed / Cancelled. */
    @Column(length = 30)
    private String status;

    @Column(name = "customer_id")
    private Long customerId;

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

    @ElementCollection
    @CollectionTable(name = "finance_sales_order_line", joinColumns = @JoinColumn(name = "sales_order_id"))
    @Builder.Default
    private List<FinanceLineItem> lines = new ArrayList<>();

}
