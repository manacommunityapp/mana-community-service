package com.manacommunity.api.groupbuying.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class CreateDemandRequest {
    @NotBlank
    private String title;
    @NotBlank
    private String category;
    private String description;
    private Integer expectedQty;
    private BigDecimal preferredPriceMin;
    private BigDecimal preferredPriceMax;
    private String preferredBrand;
}
