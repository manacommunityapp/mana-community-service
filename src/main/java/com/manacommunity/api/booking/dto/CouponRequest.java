package com.manacommunity.api.booking.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class CouponRequest {

    @NotBlank
    private String code;

    @NotNull
    private String discountType;

    @NotNull
    private BigDecimal discountValue;

    private String description;
    private BigDecimal minimumAmount;
    private BigDecimal maximumDiscount;
    private Integer maxUsageCount;
    private String validFrom;
    private String validTo;
    private Boolean isActive;
}
