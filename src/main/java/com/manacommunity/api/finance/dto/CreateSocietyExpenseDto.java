package com.manacommunity.api.finance.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class CreateSocietyExpenseDto {
    @NotBlank private String category;
    @NotBlank private String title;
    private String description;
    @NotNull private BigDecimal amount;
    @NotBlank private String accountId;
    private String vendorId;
    private String receiptUrl;
}
