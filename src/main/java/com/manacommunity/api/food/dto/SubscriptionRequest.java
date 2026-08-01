package com.manacommunity.api.food.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

@Data
public class SubscriptionRequest {
    @NotNull
    private Long planId;
    private LocalDate startDate;
    private String deliveryAddress;
    private String deliveryInstructions;
    private String paymentMethod;
    private Boolean autoRenew;
}
