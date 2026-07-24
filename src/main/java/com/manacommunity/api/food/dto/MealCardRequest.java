package com.manacommunity.api.food.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class MealCardRequest {
    @NotNull
    private Long accountId;
    @NotNull
    private Long userId;
    private BigDecimal dailyLimit;
    private BigDecimal monthlyLimit;
    private LocalDate validFrom;
    private LocalDate validUntil;
}
