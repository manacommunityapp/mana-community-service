package com.manacommunity.api.commerce.core.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class CouponApplyRequest {
    private String couponCode;
    private BigDecimal orderAmount;
}
