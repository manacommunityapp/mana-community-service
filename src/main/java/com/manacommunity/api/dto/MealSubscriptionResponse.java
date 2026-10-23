package com.manacommunity.api.dto;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MealSubscriptionResponse {
    private Long id;
    private Long userId;
    private String userName;
    private MealPlanResponse mealPlan;
    private LocalDate startDate;
    private LocalDate endDate;
    private String status;
    private String deliveryAddress;
    private String specialInstructions;
    private LocalDateTime createdAt;
}
