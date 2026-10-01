package com.manacommunity.api.finance.personal.dto;

import lombok.*;
import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreatePersonalTransactionDto {
    private String type; // INCOME, EXPENSE, TRANSFER
    private BigDecimal amount;
    private String categoryId;
    private String subcategoryId;
    private String subcategoryName;
    private String accountId;
    private String toAccountId;
    private String description;
    private String notes;
    private String receiptUrl;
    private String tags;
    private String splitDetails;
    private String date; // YYYY-MM-DD
}
