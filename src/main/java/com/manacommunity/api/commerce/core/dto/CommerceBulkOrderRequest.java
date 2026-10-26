package com.manacommunity.api.commerce.core.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class CommerceBulkOrderRequest {
    private Long vendorUserId;
    private String itemsJson;
    private String deliveryAddress;
    private String requestedDate;
    private BigDecimal quotedAmount;
    private String customerNote;
    private String adminNote;
    private String vendorNote;
}
