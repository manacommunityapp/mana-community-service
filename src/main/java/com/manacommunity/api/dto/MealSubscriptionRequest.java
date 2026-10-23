package com.manacommunity.api.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

@Data
public class MealSubscriptionRequest {
    @NotNull
    private Long mealPlanId;
    @NotNull
    private LocalDate startDate;
    @NotNull
    private LocalDate endDate;
    private String deliveryAddress;
    private String specialInstructions;
}
