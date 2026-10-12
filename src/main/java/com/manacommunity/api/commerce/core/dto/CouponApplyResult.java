package com.manacommunity.api.commerce.core.dto;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;

@Data @Builder
public class CouponApplyResult {
    private boolean valid;
    private String message;
    private BigDecimal discountAmount;
    private BigDecimal finalAmount;
    private String couponCode;
    private String couponType;
}
