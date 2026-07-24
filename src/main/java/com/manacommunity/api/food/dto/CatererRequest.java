package com.manacommunity.api.food.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class CatererRequest {
    @NotBlank
    private String name;
    private String description;
    private String cuisineTypes;
    private Integer minOrderCount;
    private Integer maxOrderCount;
    private BigDecimal pricePerPlateFrom;
    private BigDecimal pricePerPlateTo;
    private String fssaiLicense;
    private String logoUrl;
}
