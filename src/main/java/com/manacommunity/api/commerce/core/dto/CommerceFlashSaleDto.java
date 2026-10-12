package com.manacommunity.api.commerce.core.dto;

import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;
import java.util.List;

@Value
@Builder
public class CommerceFlashSaleDto {
    Long id;
    String title;
    String description;
    String startsAt;
    String endsAt;
    String discountType;
    BigDecimal discountValue;
    Integer maxQuantity;
    String status;
    List<ItemDto> items;
    boolean active;

    @Value
    @Builder
    public static class ItemDto {
        Long id;
        Long productId;
        String productTitle;
        String productSku;
        String thumbnailUrl;
        BigDecimal originalPrice;
        BigDecimal flashPrice;
        Integer quantityLimit;
        Integer quantitySold;
    }
}
