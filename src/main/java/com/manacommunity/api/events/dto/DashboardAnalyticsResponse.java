package com.manacommunity.api.events.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardAnalyticsResponse {

    private List<DailyRegistrationPoint> dailyRegistrations;
    private List<PassCategoryPoint> passCategories;
    private List<ScheduleDutyPoint> todaysScheduleDuty;
    private List<BudgetExpensePoint> budgetVsExpenses;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DailyRegistrationPoint {
        private String day;
        private long count;
        private long vip;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PassCategoryPoint {
        private String name;
        private long value;
        private String color;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ScheduleDutyPoint {
        private String time;
        private long programs;
        private long volunteers;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BudgetExpensePoint {
        private String cat;
        private double budget;
        private double spent;
    }
}
