package com.manacommunity.api.marketplace.dto;

import com.manacommunity.api.marketplace.entity.MarketCoupon;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class MarketCouponDto {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Request {
        @NotBlank(message = "Coupon code is required")
        private String code;
        @NotBlank(message = "Description is required")
        private String description;
        @NotNull(message = "Discount type is required")
        private MarketCoupon.DiscountType discountType;
        @NotNull(message = "Discount value is required")
        @DecimalMin(value = "0.01")
        private BigDecimal discountValue;
        private BigDecimal minOrderAmount;
        private BigDecimal maxDiscountAmount;
        @NotNull(message = "Valid from date is required")
        private LocalDateTime validFrom;
        @NotNull(message = "Valid to date is required")
        private LocalDateTime validTo;
        private Integer usageLimit;
        private Boolean active;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ValidateRequest {
        @NotBlank(message = "Coupon code is required")
        private String code;
        @NotNull(message = "Subtotal is required")
        private BigDecimal subtotal;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Response {
        private Long id;
        private String code;
        private String description;
        private MarketCoupon.DiscountType discountType;
        private BigDecimal discountValue;
        private BigDecimal minOrderAmount;
        private BigDecimal maxDiscountAmount;
        private LocalDateTime validFrom;
        private LocalDateTime validTo;
        private Integer usageLimit;
        private int timesUsed;
        private boolean active;
        private BigDecimal calculatedDiscount;
    }
}
