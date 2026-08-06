package com.manacommunity.api.vendor.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "vms_goods_receipt_items")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VmsGoodsReceiptItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "grn_id", nullable = false)
    private VmsGoodsReceipt goodsReceipt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "po_item_id")
    private VmsPurchaseOrderItem poItem;

    @Column(name = "item_name", nullable = false, length = 200)
    private String itemName;

    @Column(name = "ordered_quantity", nullable = false, precision = 10, scale = 2)
    private BigDecimal orderedQuantity;

    @Column(name = "received_quantity", nullable = false, precision = 10, scale = 2)
    private BigDecimal receivedQuantity;

    @Column(name = "accepted_quantity", nullable = false, precision = 10, scale = 2)
    private BigDecimal acceptedQuantity;

    @Column(name = "rejected_quantity", precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal rejectedQuantity = BigDecimal.ZERO;

    @Column(name = "rejection_reason", length = 500)
    private String rejectionReason;

    @Column(name = "sort_order", nullable = false)
    @Builder.Default
    private Integer sortOrder = 0;
}
