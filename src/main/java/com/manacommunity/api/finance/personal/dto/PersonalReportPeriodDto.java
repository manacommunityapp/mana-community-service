package com.manacommunity.api.finance.personal.dto;

import lombok.*;
import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PersonalReportPeriodDto {
    private String period;
    private String label;
    private BigDecimal totalIncome;
    private BigDecimal totalExpenses;
    private BigDecimal netSavings;
    private List<TopCategoryDto> topCategories;
    private List<MonthlyBreakdownDto> monthlyBreakdown;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TopCategoryDto {
        private String categoryId;
        private String categoryName;
        private String categoryIcon;
        private String categoryColor;
        private BigDecimal amount;
        private int percentage;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MonthlyBreakdownDto {
        private String month;
        private String label;
        private BigDecimal income;
        private BigDecimal expenses;
    }
}
