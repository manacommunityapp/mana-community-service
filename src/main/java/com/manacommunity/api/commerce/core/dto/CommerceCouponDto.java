package com.manacommunity.api.commerce.core.dto;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;

@Data @Builder
public class CommerceCouponDto {
    private Long id;
    private String couponCode;
    private String title;
    private String description;
    private String couponType;
    private BigDecimal value;
    private BigDecimal minOrderAmount;
    private BigDecimal maxDiscount;
    private Long vendorUserId;
    private String vendorName;
    private String category;
    private String channel;
    private Integer maxUses;
    private Integer usedCount;
    private Integer maxUsesPerUser;
    private String validFrom;
    private String validTo;
    private boolean active;
    private String createdAt;
}
