package com.manacommunity.api.groupbuy.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import lombok.Data;

@Data
public class JoinDealRequest {

    @Positive
    private int quantity = 1;

    @NotBlank
    private String fulfillment; // PICKUP | DELIVERY

    private String paymentMethod = "UPI";
    private String pickupPoint;
    private String deliveryAddress;
}
