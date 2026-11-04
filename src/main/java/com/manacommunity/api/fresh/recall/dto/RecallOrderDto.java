package com.manacommunity.api.fresh.recall.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecallOrderDto {
    private Long id;
    private Long recallId;
    private Long orderId;
    private String orderNumber;
    private Long customerId;
    private String customerName;
    private Long fulfilmentItemId;
    private BigDecimal quantity;
    private BigDecimal refundAmount;
    private Boolean customerNotified;
    private Boolean refundProcessed;
    private Instant notificationSentAt;
    private Instant createdAt;
}
