package com.manacommunity.api.wellness.dto;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Single-call aggregation of all fitness data for one day.
 * Returned by GET /api/wellness/fitness-summary?date=
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DailyFitnessSummaryDto {

    private LocalDate date;

    // ── Nutrition ──────────────────────────────────────────────
    private Integer totalCalories;
    private Integer calorieTarget;
    private Double totalProtein;
    private Double totalCarbs;
    private Double totalFat;
    private Double totalFiber;
    private Integer totalWaterMl;
    private Integer waterTargetMl;
    private Integer mealCount;

    // ── Exercise ───────────────────────────────────────────────
    private Integer caloriesBurned;
    private Integer netCalories;
    private Integer workoutCount;
    private Integer totalWorkoutMinutes;

    // ── Steps ──────────────────────────────────────────────────
    private Integer stepCount;
    private Integer stepGoal;
    private Boolean stepGoalMet;
    private BigDecimal distanceKm;
    private Integer activeMinutes;
    private Integer activeMinutesGoal;

    // ── Streak ─────────────────────────────────────────────────
    private Integer currentStreakDays;
    private Integer longestStreakDays;
    private Integer weeklyWorkoutsCompleted;
    private Integer weeklyWorkoutsTarget;

    // ── Body Metrics (latest on file) ──────────────────────────
    private BigDecimal latestWeightKg;
    private BigDecimal latestBmi;

    // ── Composite fitness score 0–100 ─────────────────────────
    // 25 pts each: calories on target (±20%), step goal met,
    //              at least 1 workout, water goal 80% met
    private Integer fitnessScore;
}
