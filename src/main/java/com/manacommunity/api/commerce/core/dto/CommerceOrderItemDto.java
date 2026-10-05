package com.manacommunity.api.commerce.core.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommerceOrderItemDto {
    private String productId;
    private String variantId;
    private String sku;
    private String title;
    private String packSize;
    private BigDecimal unitPrice;
    private Integer quantity;
    private BigDecimal totalPrice;
    private String imageUrl;
    private String thumbnailUrl;
}