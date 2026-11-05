package com.manacommunity.api.wellness.dto;

import lombok.*;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FitnessStreakDto {
    private Long userId;
    private Integer currentStreakDays;
    private Integer longestStreakDays;
    private LocalDate lastActivityDate;
    private Integer totalActiveDays;
    private Integer weeklyWorkoutsCompleted;
    private Integer weeklyWorkoutsTarget;
    private Integer todaySteps;
    private Integer stepGoal;
}
