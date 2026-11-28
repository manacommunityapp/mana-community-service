package com.manacommunity.api.mobility.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;

@Data @Builder
public class MobilityReceiptDto {
    private String receiptNumber;
    private Long tripId;
    private String tripRef;
    private String serviceType;
    private String paymentMethod;
    private String paymentStatus;
    private String gateway;
    private String paymentOrderId;
    private String paymentRef;
    private BigDecimal baseFare;
    private BigDecimal distanceFare;
    private BigDecimal durationFare;
    private BigDecimal taxes;
    private BigDecimal totalPaid;
    private String refundStatus;
    private BigDecimal refundAmount;
    private Instant paidAt;
    private String pickupAddress;
    private String destAddress;
    private String passengerName;
    private String driverName;
}
