package com.manacommunity.api.commerce.core.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class CommerceReturnRequest {
    private String orderNumber;
    private String returnType;  // RETURN or EXCHANGE
    private String reason;
    private String description;
    private String itemsJson;
    private BigDecimal refundAmount;
}
