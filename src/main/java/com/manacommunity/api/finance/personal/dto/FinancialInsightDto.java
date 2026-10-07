package com.manacommunity.api.finance.personal.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FinancialInsightDto {
    private String id;
    private String type; // SAVINGS_OPPORTUNITY, BUDGET_OVERRUN_RISK, HIGH_SPEND_ALERT, RECURRING_REMINDER, HEALTH_SCORE
    private String title;
    private String description;
    private String severity; // INFO, SUCCESS, WARNING, CRITICAL
    private BigDecimal potentialSavings;
    private String category;
    private String actionLabel;
    private String actionRoute;
}
