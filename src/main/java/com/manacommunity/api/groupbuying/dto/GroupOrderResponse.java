package com.manacommunity.api.groupbuying.dto;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GroupOrderResponse {
    private Long id;
    private String orderNumber;
    private Long dealId;
    private String dealTitle;
    private Integer quantity;
    private BigDecimal unitPrice;
    private BigDecimal totalAmount;
    private BigDecimal savingsAmount;
    private String status;
    private String paymentStatus;
    private String paymentMethod;
    private String transactionId;
    private String qrToken;
    private String pickupPoint;
    private LocalDateTime pickupDate;
    private String deliveryAddress;
    private String deliveryOtp;
    private String deliveryPartnerName;
    private String deliveryPartnerPhone;
    private String trackingNumber;
    private LocalDateTime deliveryTimestamp;
    private BigDecimal refundAmount;
    private String refundReason;
    private BigDecimal tierPriceRefundAmount;
    private String collectorPin;
    private String collectorName;
    private String collectorRelation;
    private LocalDateTime createdAt;
}
