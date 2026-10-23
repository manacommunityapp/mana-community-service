package com.manacommunity.api.finance.personal.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GoalContributionDto {
    @NotNull
    @DecimalMin("1.00")
    private BigDecimal amount;

    private String accountId;
    private String notes;
}
