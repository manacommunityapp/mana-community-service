package com.manacommunity.api.commerce.core.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HandoverVerificationResponse {
    private Boolean verified;
    private String orderNumber;
    private String channel;
    private String buyerName;
    private String buyerFlat;
    private String itemSummary;
    private Integer totalQuantity;
    private BigDecimal totalAmount;
    private String status;
    private String message;
}