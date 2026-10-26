package com.manacommunity.api.commerce.core.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class CommerceFlashSaleRequest {
    @NotBlank
    private String title;
    private String description;
    @NotNull
    private String startsAt;   // ISO datetime string
    @NotNull
    private String endsAt;
    private String discountType = "PERCENT";
    @NotNull
    private BigDecimal discountValue;
    private Integer maxQuantity;
    private List<Long> productIds;  // products to include
}
