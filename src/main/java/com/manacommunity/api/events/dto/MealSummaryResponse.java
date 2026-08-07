package com.manacommunity.api.events.dto;

import lombok.Data;

import java.util.List;

@Data
public class MealSummaryResponse {
    private Long eventId;
    private List<DaySummary> days;

    @Data
    public static class DaySummary {
        private String date;
        private MealBreakdown lunch;
        private MealBreakdown dinner;
    }

    @Data
    public static class MealBreakdown {
        private int totalHeads;
        private int veg;
        private int vegan;
        private int jain;
        private int nonveg;
    }
}
