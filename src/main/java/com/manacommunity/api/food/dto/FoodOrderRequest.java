package com.manacommunity.api.food.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class FoodOrderRequest {
    @NotNull
    private String providerType;
    @NotNull
    private Long providerId;
    private String orderType;
    private String deliveryAddress;
    private Double deliveryLatitude;
    private Double deliveryLongitude;
    private String deliveryInstructions;
    private LocalDateTime scheduledFor;
    private Boolean isGift;
    private String giftMessage;
    private String paymentMethod;
    private List<OrderItemRequest> items;
    private String couponCode;
}
