package com.manacommunity.api.commerce.core.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class CommerceCouponRequest {
    private String couponCode;
    private String title;
    private String description;
    private String couponType;
    private BigDecimal value;
    private BigDecimal minOrderAmount;
    private BigDecimal maxDiscount;
    private Long vendorUserId;
    private String category;
    private String channel;
    private Integer maxUses;
    private Integer maxUsesPerUser;
    private String validFrom;
    private String validTo;
    private Boolean active;
}
