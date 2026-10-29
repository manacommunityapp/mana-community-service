package com.manacommunity.api.commerce.core.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "commerce_flash_sale_item",
       uniqueConstraints = @UniqueConstraint(columnNames = {"flash_sale_id", "product_id"}))
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommerceFlashSaleItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "flash_sale_id", nullable = false)
    private CommerceFlashSale flashSale;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private CommerceProduct product;

    @Column(name = "original_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal originalPrice;

    @Column(name = "flash_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal flashPrice;

    @Column(name = "quantity_limit")
    private Integer quantityLimit;

    @Column(name = "quantity_sold", nullable = false)
    @Builder.Default
    private Integer quantitySold = 0;

    @Version
    @Column(nullable = false)
    @Builder.Default
    private Long version = 0L;
}
