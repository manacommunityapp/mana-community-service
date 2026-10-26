package com.manacommunity.api.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "procurement_po_items")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class POItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "po_id", nullable = false)
    @JsonIgnore
    private ProcurementPO purchaseOrder;

    @Column(name = "product_name", nullable = false, length = 200)
    private String productName;

    @Column(nullable = false, length = 40)
    @Builder.Default
    private String unit = "units";

    @Column(nullable = false)
    private Integer quantity;

    @Column(name = "price_per_unit", nullable = false, precision = 12, scale = 2)
    private BigDecimal pricePerUnit;

    @Column(name = "gst_percent", nullable = false, precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal gstPercent = BigDecimal.valueOf(18);

    @Column(name = "total_price", nullable = false, precision = 14, scale = 2)
    private BigDecimal totalPrice;

    @Column(name = "lead_time_days")
    @Builder.Default
    private Integer leadTimeDays = 3;

    @Column(length = 400)
    private String notes;
}
