package com.manacommunity.api.finance.personal.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.*;
import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreatePersonalRecurringDto {
    @NotBlank(message = "Recurring rule name is required")
    private String name;

    @NotBlank(message = "Type is required")
    @Pattern(regexp = "INCOME|EXPENSE|TRANSFER", message = "Type must be INCOME, EXPENSE, or TRANSFER")
    private String type;

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be greater than zero")
    private BigDecimal amount;

    private String categoryId;

    @NotBlank(message = "Account ID is required")
    private String accountId;

    @NotBlank(message = "Frequency is required")
    @Pattern(regexp = "DAILY|WEEKLY|MONTHLY|YEARLY", message = "Frequency must be DAILY, WEEKLY, MONTHLY, or YEARLY")
    private String frequency;

    private String nextDueDate;
}
