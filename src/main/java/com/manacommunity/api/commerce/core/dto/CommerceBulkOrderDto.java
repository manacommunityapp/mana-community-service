package com.manacommunity.api.commerce.core.dto;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;

@Data @Builder
public class CommerceBulkOrderDto {
    private Long id;
    private String bulkOrderCode;
    private Long customerId;
    private String customerName;
    private Long vendorId;
    private String vendorName;
    private String itemsJson;
    private BigDecimal quotedAmount;
    private BigDecimal finalAmount;
    private String deliveryAddress;
    private String requestedDate;
    private String status;
    private String customerNote;
    private String adminNote;
    private String vendorNote;
    private String createdAt;
}
