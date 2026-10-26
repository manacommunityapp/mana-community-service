package com.manacommunity.api.commerce.core.dto;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;

@Data @Builder
public class CommerceReturnDto {
    private Long id;
    private String returnCode;
    private Long orderId;
    private String orderNumber;
    private Long customerId;
    private String customerName;
    private String returnType;
    private String reason;
    private String description;
    private String itemsJson;
    private BigDecimal refundAmount;
    private String status;
    private String adminNote;
    private String resolvedByName;
    private String resolvedAt;
    private String createdAt;
}
