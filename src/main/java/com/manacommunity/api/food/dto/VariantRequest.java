package com.manacommunity.api.food.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class VariantRequest {
    private String variantName;
    private BigDecimal price;
    private Boolean isDefault;
}
