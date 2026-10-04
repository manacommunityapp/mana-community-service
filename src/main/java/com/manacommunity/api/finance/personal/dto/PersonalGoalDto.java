package com.manacommunity.api.finance.personal.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PersonalGoalDto {
    private String id;
    private String name;
    private BigDecimal targetAmount;
    private BigDecimal currentAmount;
    private BigDecimal remainingAmount;
    private Integer percentAchieved;
    private BigDecimal requiredMonthlySavings;
    private Integer monthsRemaining;
    private LocalDate targetDate;
    private String icon;
    private String color;
    private String categoryId;
    private String notes;
    private Boolean isCompleted;
}
