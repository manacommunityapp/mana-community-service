package com.manacommunity.api.finance.personal.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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
public class CreatePersonalGoalDto {
    @NotBlank
    private String name;

    @NotNull
    @DecimalMin("1.00")
    private BigDecimal targetAmount;

    private BigDecimal currentAmount;
    private LocalDate targetDate;
    private String icon;
    private String color;
    private String categoryId;
    private String notes;
}
