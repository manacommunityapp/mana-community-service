package com.manacommunity.api.events.dto;

import lombok.Data;

import java.util.List;

@Data
public class MealRegistrationResponse {
    private Long eventId;
    private Long userId;
    private String dietaryPref;
    private String allergies;
    private List<DayMealResponse> meals;

    @Data
    public static class DayMealResponse {
        private String date;
        private boolean lunch;
        private boolean dinner;
        private int headCount;
    }
}
