package com.manacommunity.api.finance.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * A single line item on a financial document. Reused (as an {@code @Embeddable})
 * by every dedicated document entity — invoices, estimates, sales orders, credit
 * notes, purchases, purchase orders and debit notes — each of which maps it into
 * its OWN line table via {@code @CollectionTable}, so the menus stay fully separate.
 */
@Embeddable
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FinanceLineItem {

    @Column(length = 200)
    private String item;

    @Column(columnDefinition = "text")
    private String description;

    private Integer qty;

    @Column(precision = 14, scale = 2)
    private BigDecimal cost;

    /** Discount percent. */
    @Column(precision = 14, scale = 2)
    private BigDecimal disc;

    /** Tax percent. */
    @Column(precision = 14, scale = 2)
    private BigDecimal tax;

    @Column(name = "line_total", precision = 14, scale = 2)
    private BigDecimal lineTotal;
}
