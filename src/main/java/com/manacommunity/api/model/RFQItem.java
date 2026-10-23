package com.manacommunity.api.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "rfq_items")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RFQItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rfq_id", nullable = false)
    @JsonIgnore
    private ProcurementRFQ rfq;

    @Column(name = "product_name", nullable = false, length = 200)
    private String productName;

    @Column(length = 600)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    @Builder.Default
    private ProcurementCategory category = ProcurementCategory.OTHER;

    @Column(nullable = false, length = 40)
    @Builder.Default
    private String unit = "units";

    @Column(nullable = false)
    private Integer quantity;

    @Column(name = "target_price_per_unit", precision = 12, scale = 2)
    private BigDecimal targetPricePerUnit;

    @Column(length = 600)
    private String specifications;
}
