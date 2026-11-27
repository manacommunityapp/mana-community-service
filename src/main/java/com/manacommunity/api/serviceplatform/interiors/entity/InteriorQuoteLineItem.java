package com.manacommunity.api.serviceplatform.interiors.entity;

import com.manacommunity.api.serviceplatform.pricing.entity.ServiceQuote;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "sp_service_quote_line_item", indexes = {
        @Index(name = "idx_interior_quote_line_quote", columnList = "service_quote_id,position")
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class InteriorQuoteLineItem {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "service_quote_id", nullable = false)
    private ServiceQuote quote;

    @Column(nullable = false, length = 40)
    private String category;

    @Column(nullable = false, length = 240)
    private String description;

    @Column(nullable = false, precision = 12, scale = 3)
    private BigDecimal quantity;

    @Column(nullable = false, length = 24)
    private String unit;

    @Column(name = "unit_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal unitPrice;

    @Column(name = "margin_percent", nullable = false, precision = 6, scale = 2)
    private BigDecimal marginPercent;

    @Column(name = "tax_percent", nullable = false, precision = 6, scale = 2)
    private BigDecimal taxPercent;

    @Column(name = "line_total", nullable = false, precision = 12, scale = 2)
    private BigDecimal lineTotal;

    @Column(nullable = false)
    private Integer position;

    @Column(name = "catalog_product_id")
    private Long catalogProductId;

    @Column(name = "procurement_quantity")
    private Integer procurementQuantity;
}
