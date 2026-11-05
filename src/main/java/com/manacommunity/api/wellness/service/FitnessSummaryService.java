package com.manacommunity.api.wellness.service;

import com.manacommunity.api.wellness.dto.DailyFitnessSummaryDto;
import com.manacommunity.api.wellness.dto.DailyNutritionSummaryDto;
import com.manacommunity.api.wellness.dto.WeeklyFitnessPointDto;
import com.manacommunity.api.wellness.entity.ActivityGoal;
import com.manacommunity.api.wellness.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class FitnessSummaryService {

    private final WellnessService wellnessService;
    private final WorkoutLogRepository workoutLogRepository;
    private final StepLogRepository stepLogRepository;
    private final ActivityGoalRepository activityGoalRepository;
    private final FitnessStreakRepository fitnessStreakRepository;
    private final BodyMetricLogRepository bodyMetricLogRepository;

    // ── Daily aggregation ─────────────────────────────────────

    public DailyFitnessSummaryDto getDailySummary(Long userId, LocalDate date) {
        // Nutrition
        DailyNutritionSummaryDto nutrition = wellnessService.getDailySummary(userId, date);

        // Goals
        ActivityGoal goal = activityGoalRepository.findByUserId(userId).orElse(null);
        int stepGoal = goal != null && goal.getDailyStepsTarget() != null ? goal.getDailyStepsTarget() : 8000;
        int activeMinutesGoal = goal != null && goal.getActiveMinutesTarget() != null ? goal.getActiveMinutesTarget() : 30;
        int weeklyWorkoutsTarget = goal != null && goal.getWeeklyWorkoutsTarget() != null ? goal.getWeeklyWorkoutsTarget() : 4;

        // Workout stats
        Integer workoutCount = workoutLogRepository.getWorkoutCountForDate(userId, date);
        Integer totalWorkoutMinutes = workoutLogRepository.getTotalWorkoutMinutesForDate(userId, date);

        // Steps
        Integer stepCount = stepLogRepository.getTotalStepsForDate(userId, date);
        BigDecimal distanceKm = stepLogRepository.findByUserIdAndLogDate(userId, date)
                .map(s -> s.getDistanceKm())
                .orElse(null);
        Integer activeMinutes = stepLogRepository.findByUserIdAndLogDate(userId, date)
                .map(s -> s.getActiveMinutes())
                .orElse(null);

        // Streak
        var streak = fitnessStreakRepository.findByUserId(userId).orElse(null);
        LocalDate weekStart = date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        Integer weeklyCompleted = workoutLogRepository.getWorkoutCountForDateRange(userId, weekStart, date);

        // Latest body metrics
        BigDecimal latestWeightKg = null;
        BigDecimal latestBmi = null;
        var latestMetric = bodyMetricLogRepository.findFirstByUserIdOrderByLoggedDateDesc(userId);
        if (latestMetric.isPresent()) {
            latestWeightKg = latestMetric.get().getWeightKg();
            latestBmi = latestMetric.get().getBmi();
        }

        // Fitness score (25 pts each of 4 pillars)
        int score = 0;
        int calorieTarget = nutrition.getCalorieTarget();
        int calories = nutrition.getTotalCalories();
        if (calorieTarget > 0 && calories > 0) {
            double ratio = (double) calories / calorieTarget;
            if (ratio >= 0.8 && ratio <= 1.2) score += 25;
        }
        if (stepCount != null && stepGoal > 0 && stepCount >= stepGoal) score += 25;
        if (workoutCount != null && workoutCount > 0) score += 25;
        int waterTarget = nutrition.getWaterTargetMl();
        if (waterTarget > 0 && nutrition.getTotalWaterMl() >= waterTarget * 0.8) score += 25;

        return DailyFitnessSummaryDto.builder()
                .date(date)
                // Nutrition
                .totalCalories(nutrition.getTotalCalories())
                .calorieTarget(nutrition.getCalorieTarget())
                .totalProtein(nutrition.getTotalProtein())
                .totalCarbs(nutrition.getTotalCarbs())
                .totalFat(nutrition.getTotalFat())
                .totalFiber(nutrition.getTotalFiber())
                .totalWaterMl(nutrition.getTotalWaterMl())
                .waterTargetMl(nutrition.getWaterTargetMl())
                .mealCount(nutrition.getMealCount())
                // Exercise
                .caloriesBurned(nutrition.getCaloriesBurned())
                .netCalories(nutrition.getNetCalories())
                .workoutCount(workoutCount != null ? workoutCount : 0)
                .totalWorkoutMinutes(totalWorkoutMinutes != null ? totalWorkoutMinutes : 0)
                // Steps
                .stepCount(stepCount != null ? stepCount : 0)
                .stepGoal(stepGoal)
                .stepGoalMet(stepCount != null && stepCount >= stepGoal)
                .distanceKm(distanceKm)
                .activeMinutes(activeMinutes)
                .activeMinutesGoal(activeMinutesGoal)
                // Streak
                .currentStreakDays(streak != null ? streak.getCurrentStreakDays() : 0)
                .longestStreakDays(streak != null ? streak.getLongestStreakDays() : 0)
                .weeklyWorkoutsCompleted(weeklyCompleted != null ? weeklyCompleted : 0)
                .weeklyWorkoutsTarget(weeklyWorkoutsTarget)
                // Body
                .latestWeightKg(latestWeightKg)
                .latestBmi(latestBmi)
                // Score
                .fitnessScore(score)
                .build();
    }

    // ── 7-day trend ───────────────────────────────────────────

    public List<WeeklyFitnessPointDto> getWeeklyTrend(Long userId, LocalDate endDate) {
        List<WeeklyFitnessPointDto> result = new ArrayList<>();
        ActivityGoal goal = activityGoalRepository.findByUserId(userId).orElse(null);
        int stepGoal = goal != null && goal.getDailyStepsTarget() != null ? goal.getDailyStepsTarget() : 8000;

        for (int i = 6; i >= 0; i--) {
            LocalDate d = endDate.minusDays(i);
            DailyNutritionSummaryDto n = wellnessService.getDailySummary(userId, d);
            Integer steps = stepLogRepository.getTotalStepsForDate(userId, d);
            Integer workouts = workoutLogRepository.getWorkoutCountForDate(userId, d);
            Integer burned = workoutLogRepository.getTotalCaloriesBurnedForDate(userId, d);

            int score = 0;
            if (n.getCalorieTarget() > 0 && n.getTotalCalories() > 0) {
                double r = (double) n.getTotalCalories() / n.getCalorieTarget();
                if (r >= 0.8 && r <= 1.2) score += 25;
            }
            if (steps != null && steps >= stepGoal) score += 25;
            if (workouts != null && workouts > 0) score += 25;
            if (n.getWaterTargetMl() > 0 && n.getTotalWaterMl() >= n.getWaterTargetMl() * 0.8) score += 25;

            result.add(WeeklyFitnessPointDto.builder()
                    .date(d)
                    .caloriesConsumed(n.getTotalCalories())
                    .caloriesBurned(burned != null ? burned : 0)
                    .stepCount(steps != null ? steps : 0)
                    .workoutCount(workouts != null ? workouts : 0)
                    .fitnessScore(score)
                    .build());
        }
        return result;
    }
}
