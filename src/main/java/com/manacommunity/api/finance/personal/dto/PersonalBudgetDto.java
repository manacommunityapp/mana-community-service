package com.manacommunity.api.finance.personal.dto;

import lombok.*;
import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PersonalBudgetDto {
    private String id;
    private String categoryId;
    private String categoryName;
    private String categoryIcon;
    private String categoryColor;
    private String period;
    private BigDecimal limitAmount;
    private BigDecimal spentAmount;
    private BigDecimal remainingAmount;
    private int percentUsed;
    private int alertThreshold;
    private String month;
    private boolean isOverspent;
}
