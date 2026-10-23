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
public class CreatePersonalInstallmentDto {
    @NotBlank
    private String name;

    @NotNull
    @DecimalMin("1.00")
    private BigDecimal totalAmount;

    @NotNull
    @DecimalMin("1.00")
    private BigDecimal monthlyEmi;

    private BigDecimal interestRate;

    @NotNull
    private Integer totalTenorMonths;

    private Integer remainingTenorMonths;

    @NotNull
    private LocalDate startDate;

    private LocalDate nextDueDate;
    private String accountId;
    private String categoryId;
    private Boolean isAutoDeduct;
}
