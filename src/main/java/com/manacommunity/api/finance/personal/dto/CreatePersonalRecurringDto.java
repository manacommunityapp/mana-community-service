package com.manacommunity.api.finance.personal.dto;

import lombok.*;
import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreatePersonalRecurringDto {
    private String name;
    private String type; // INCOME, EXPENSE, TRANSFER
    private BigDecimal amount;
    private String categoryId;
    private String accountId;
    private String frequency; // DAILY, WEEKLY, MONTHLY, YEARLY
    private String nextDueDate;
}
