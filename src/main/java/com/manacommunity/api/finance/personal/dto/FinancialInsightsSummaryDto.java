package com.manacommunity.api.finance.personal.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FinancialInsightsSummaryDto {
    private int healthScore; // 0 - 100
    private String healthGrade; // A+, A, B, C, D
    private String summaryMessage;
    private BigDecimal monthlyProjectedSavings;
    private List<FinancialInsightDto> insights;
    private Map<String, Object> metrics;
}
