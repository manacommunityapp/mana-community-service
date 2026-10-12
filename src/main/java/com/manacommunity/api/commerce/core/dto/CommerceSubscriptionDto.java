package com.manacommunity.api.commerce.core.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class CommerceSubscriptionDto {
    private Long id;
    private String subscriptionCode;
    private Long customerId;
    private String customerName;
    private Long productId;
    private String productName;
    private String sku;
    private Long sellerId;
    private String sellerName;
    private BigDecimal unitPrice;
    private Integer quantity;
    private BigDecimal totalPerCycle;
    private String channel;
    private String frequency;
    private String status;
    private String nextRunDate;
    private String lastRunDate;
    private Integer totalCycles;
    private Integer completedCycles;
    private String deliveryAddress;
    private String notes;
    private String createdAt;
}
