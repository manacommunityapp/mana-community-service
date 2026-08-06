package com.manacommunity.api.food.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class AddonRequest {
    private String addonGroupName;
    private String addonName;
    private BigDecimal price;
    private Boolean isDefault;
    private Integer maxQuantity;
}
