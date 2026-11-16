package com.manacommunity.api.wellness.dto;

import lombok.*;

import java.time.LocalDate;

/** One day's worth of data for the 7-day trend chart. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WeeklyFitnessPointDto {
    private LocalDate date;
    private Integer caloriesConsumed;
    private Integer caloriesBurned;
    private Integer stepCount;
    private Integer workoutCount;
    private Integer fitnessScore;
}
