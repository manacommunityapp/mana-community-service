package com.manacommunity.api.finance.personal.dto;

import lombok.*;
import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PersonalTransactionDto {
    private String id;
    private String type; // INCOME, EXPENSE, TRANSFER
    private BigDecimal amount;
    private String currency;
    private String categoryId;
    private String categoryName;
    private String categoryIcon;
    private String categoryColor;
    private String subcategoryName;
    private String accountId;
    private String accountName;
    private String toAccountId;
    private String toAccountName;
    private String description;
    private String notes;
    private String date; // YYYY-MM-DD
    private boolean isManaProjection;
    private String sourceModule;
    private String sourceType;
    private String sourceId;
    private String sourceLabel;
    private String receiptUrl;
    private String tags;
    private String splitDetails;
    private String createdAt;
}
