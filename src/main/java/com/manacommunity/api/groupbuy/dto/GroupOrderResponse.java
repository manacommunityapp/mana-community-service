package com.manacommunity.api.groupbuy.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class GroupOrderResponse {

    private Long id;
    private String orderNumber;
    private Long dealId;
    private String dealTitle;
    private Integer quantity;
    private BigDecimal pricePerUnit;
    private BigDecimal totalAmount;
    private String status;
    private String fulfillment;
    private String paymentMethod;
    private String pickupPoint;
    private String deliveryAddress;
    private String qrCode;
    private Boolean reviewed;
    private LocalDateTime orderedAt;
    private String vendorName;
}
