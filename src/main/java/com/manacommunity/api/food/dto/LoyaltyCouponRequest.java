package com.manacommunity.api.food.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class LoyaltyCouponRequest {
    @NotBlank
    private String code;
    private String title;
    private String description;
    private String discountType;
    private BigDecimal discountValue;
    private BigDecimal minOrder;
    private BigDecimal maxDiscount;
    private LocalDateTime validFrom;
    private LocalDateTime validUntil;
    private Integer usageLimit;
    private String applicableTo;
    private Long providerId;
}
