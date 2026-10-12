package com.manacommunity.api.commerce.core.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class CommerceSubscriptionRequest {
    private Long productId;
    private String productName;
    private String sku;
    private Long sellerId;
    private String sellerName;
    private BigDecimal unitPrice;
    private Integer quantity;
    private String channel;
    private String frequency;   // DAILY WEEKLY FORTNIGHTLY MONTHLY
    private String startDate;   // ISO date yyyy-MM-dd (optional, defaults to today)
    private Integer totalCycles; // null = infinite
    private String deliveryAddress;
    private String notes;
}
