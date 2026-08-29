package com.manacommunity.api.events.dto;

import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class MealRegistrationRequest {
    private Long eventId;
    private String dietaryPref;
    private String allergies;
    private List<DayMeal> meals;

    @Data
    public static class DayMeal {
        private LocalDate date;
        private boolean morning;
        private boolean lunch;
        private boolean dinner;
        private int headCount;
    }
}
