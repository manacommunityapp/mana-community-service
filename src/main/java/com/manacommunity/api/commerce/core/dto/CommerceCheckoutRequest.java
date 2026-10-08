package com.manacommunity.api.commerce.core.dto;

import com.manacommunity.api.commerce.core.model.CommerceChannel;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
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
public class CommerceCheckoutRequest {
    @NotNull
    private CommerceChannel channel;
    private String channelReferenceId;
    private Long sellerId;
    private String vendorId;
    private String vendorName;
    private String sellerName;
    @NotEmpty(message = "Items cannot be empty")
    @jakarta.validation.Valid
    private List<CommerceOrderItemDto> items;
    private BigDecimal discountAmount;
    private BigDecimal deliveryFee;
    private String paymentMethod;
    private String fulfillmentType;
    private String deliveryAddress;
    private String deliverySlot;
    private String pickupPoint;
    private String pickupSlot;
}