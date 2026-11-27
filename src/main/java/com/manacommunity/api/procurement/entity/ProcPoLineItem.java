package com.manacommunity.api.procurement.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "proc_po_line_item")
@Getter @Setter @NoArgsConstructor
public class ProcPoLineItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "purchase_order_id", nullable = false)
    private ProcPurchaseOrder purchaseOrder;

    @Column(nullable = false)
    private String itemDescription;

    @Column(nullable = false)
    private int quantity;

    @Column(nullable = false)
    private BigDecimal unitPrice;

    private String unit = "NOS";

    private BigDecimal lineTotal;

    @PrePersist @PreUpdate
    void computeTotal() {
        this.lineTotal = unitPrice.multiply(BigDecimal.valueOf(quantity));
    }
}
