package com.manacommunity.api.food.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
public class MealCardResponse {
    private Long id;
    private Long accountId;
    private Long userId;
    private String userName;
    private String cardNumber;
    private BigDecimal dailyLimit;
    private BigDecimal monthlyLimit;
    private BigDecimal currentDailySpent;
    private BigDecimal currentMonthlySpent;
    private LocalDate validFrom;
    private LocalDate validUntil;
    private String status;
    private Long communityId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
