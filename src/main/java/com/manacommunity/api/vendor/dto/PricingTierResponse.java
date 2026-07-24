package com.manacommunity.api.vendor.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class PricingTierResponse {
    private Long id;
    private String name;
    private String description;
    private BigDecimal price;
    private Integer minQuantity;
    private Integer maxQuantity;
}
