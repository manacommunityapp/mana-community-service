package com.manacommunity.api.food.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class SubscriptionResponse {
    private Long id;
    private Long userId;
    private Long planId;
    private String planName;
    private String planType;
    private String status;
    private LocalDate startDate;
    private LocalDate endDate;
    private String deliveryAddress;
    private String deliveryInstructions;
    private String paymentMethod;
    private Boolean autoRenew;
    private BigDecimal amountPaid;
    private Integer mealsRemaining;
    private List<DeliveryScheduleResponse> deliverySchedule;
    private Long communityId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @Data
    @Builder
    public static class DeliveryScheduleResponse {
        private Long id;
        private LocalDate date;
        private String mealType;
        private String status;
        private String itemName;
    }
}
