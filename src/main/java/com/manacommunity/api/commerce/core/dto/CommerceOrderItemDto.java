package com.manacommunity.api.commerce.core.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
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
    @NotNull(message = "Quantity is required")
    @Min(value = 1, message = "Quantity must be at least 1")
    private Integer quantity;
    private BigDecimal totalPrice;
    private String imageUrl;
    private String thumbnailUrl;
}