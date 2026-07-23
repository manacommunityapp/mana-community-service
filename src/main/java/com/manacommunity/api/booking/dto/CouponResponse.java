package com.manacommunity.api.booking.dto;

import com.manacommunity.api.booking.entity.enums.DiscountType;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class CouponResponse {

    private Long id;
    private String code;
    private String description;
    private DiscountType discountType;
    private BigDecimal discountValue;
    private Integer maxUses;
    private Integer currentUses;
    private String validFrom;
    private String validTo;
    private BigDecimal minBookingAmount;
    private BigDecimal maxDiscountAmount;
    private Boolean isActive;
}
