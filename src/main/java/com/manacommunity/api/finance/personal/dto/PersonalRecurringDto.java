package com.manacommunity.api.finance.personal.dto;

import lombok.*;
import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PersonalRecurringDto {
    private String id;
    private String name;
    private String type;
    private BigDecimal amount;
    private String categoryId;
    private String categoryName;
    private String categoryIcon;
    private String accountId;
    private String frequency;
    private String nextDueDate;
    private boolean isActive;
}
