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
public class CreatePersonalTransactionDto {
    @NotBlank(message = "Transaction type is required")
    @Pattern(regexp = "INCOME|EXPENSE|TRANSFER", message = "Type must be INCOME, EXPENSE, or TRANSFER")
    private String type;

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be greater than zero")
    private BigDecimal amount;

    private String categoryId;
    private String subcategoryId;
    private String subcategoryName;

    @NotBlank(message = "Account ID is required")
    private String accountId;

    private String toAccountId;
    private String description;
    private String notes;
    private String receiptUrl;
    private String tags;
    private String splitDetails;
    private String date; // YYYY-MM-DD
}
