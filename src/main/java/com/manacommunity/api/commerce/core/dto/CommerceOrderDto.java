package com.manacommunity.api.commerce.core.dto;

import com.manacommunity.api.commerce.core.model.CommerceChannel;
import com.manacommunity.api.commerce.core.model.CommerceOrderStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommerceOrderDto {
    private Long id;
    private String orderNumber;
    private CommerceChannel channel;
    private Long buyerId;
    private String buyerName;
    private String buyerFlat;
    private Long sellerId;
    private String vendorId;
    private String vendorName;
    private Long communityId;
    private CommerceOrderStatus status;
    private BigDecimal subtotalAmount;
    private BigDecimal discountAmount;
    private BigDecimal deliveryFee;
    private BigDecimal taxAmount;
    private BigDecimal totalAmount;
    private BigDecimal savingsAmount;
    private String paymentMethod;
    private String paymentStatus;
    private String fulfillmentType;
    private String deliveryAddress;
    private String pickupPoint;
    private String pickupSlot;
    private String handoverOtp;
    private String qrToken;
    private Boolean isEscrowLocked;
    private List<CommerceOrderItemDto> items;
    private String createdAt;
}