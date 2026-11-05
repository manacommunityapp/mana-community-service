package com.manacommunity.api.wellness.controller;

import com.manacommunity.api.user.security.UserPrincipal;
import com.manacommunity.api.wellness.dto.*;
import com.manacommunity.api.wellness.entity.FoodCatalog;
import com.manacommunity.api.wellness.enums.WellnessEnums.*;
import com.manacommunity.api.wellness.service.FitnessGoalService;
import com.manacommunity.api.wellness.service.StepService;
import com.manacommunity.api.wellness.service.WellnessService;
import com.manacommunity.api.wellness.service.WorkoutService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/wellness")
public class WellnessApiController {

    private final WellnessService wellnessService;
    private final WorkoutService workoutService;
    private final FitnessGoalService fitnessGoalService;
    private final StepService stepService;

    public WellnessApiController(WellnessService wellnessService, WorkoutService workoutService,
                                  FitnessGoalService fitnessGoalService, StepService stepService) {
        this.wellnessService = wellnessService;
        this.workoutService = workoutService;
        this.fitnessGoalService = fitnessGoalService;
        this.stepService = stepService;
    }

    // ── Profile ──

    @GetMapping("/profile")
    public ResponseEntity<WellnessProfileDto> getProfile(@AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(wellnessService.getProfile(principal.getId()));
    }

    @PutMapping("/profile")
    public ResponseEntity<WellnessProfileDto> saveProfile(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestBody WellnessProfileDto dto) {
        return ResponseEntity.ok(wellnessService.saveProfile(principal.getId(), dto));
    }

    // ── Meals ──

    @GetMapping("/meals")
    public ResponseEntity<List<MealLogDto>> getMeals(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(wellnessService.getMealsForDate(principal.getId(), date));
    }

    @GetMapping("/meals/range")
    public ResponseEntity<List<MealLogDto>> getMealsRange(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end) {
        return ResponseEntity.ok(wellnessService.getMealsForRange(principal.getId(), start, end));
    }

    @PostMapping("/meals")
    public ResponseEntity<MealLogDto> logMeal(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestBody MealLogDto dto) {
        return ResponseEntity.ok(wellnessService.logMeal(principal.getId(), dto));
    }

    @DeleteMapping("/meals/{id}")
    public ResponseEntity<Void> deleteMeal(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id) {
        wellnessService.deleteMealLog(principal.getId(), id);
        return ResponseEntity.noContent().build();
    }

    // ── Daily Summary ──

    @GetMapping("/summary")
    public ResponseEntity<DailyNutritionSummaryDto> getDailySummary(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(wellnessService.getDailySummary(principal.getId(), date != null ? date : LocalDate.now()));
    }

    // ── Water ──

    @GetMapping("/water")
    public ResponseEntity<List<WaterLogDto>> getWaterLogs(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(wellnessService.getWaterLogsForDate(principal.getId(), date));
    }

    @PostMapping("/water")
    public ResponseEntity<WaterLogDto> logWater(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestBody WaterLogDto dto) {
        return ResponseEntity.ok(wellnessService.logWater(principal.getId(), dto));
    }

    // ── Food Catalog ──

    @GetMapping("/food/search")
    public ResponseEntity<List<FoodCatalog>> searchFood(@RequestParam String query) {
        return ResponseEntity.ok(wellnessService.searchFood(query));
    }

    // ── Challenges ──

    @GetMapping("/challenges")
    public ResponseEntity<List<WellnessChallengeDto>> getChallenges(
            @RequestParam(required = false) ChallengeStatus status) {
        return ResponseEntity.ok(wellnessService.getChallenges(status));
    }

    @PostMapping("/challenges/{id}/join")
    public ResponseEntity<ChallengeParticipantDto> joinChallenge(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id) {
        return ResponseEntity.ok(wellnessService.joinChallenge(principal.getId(), id));
    }

    @PutMapping("/challenges/{id}/progress")
    public ResponseEntity<ChallengeParticipantDto> updateProgress(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id,
            @RequestParam Integer progress) {
        return ResponseEntity.ok(wellnessService.updateProgress(principal.getId(), id, progress));
    }

    // ── Providers ──

    @GetMapping("/providers")
    public ResponseEntity<List<WellnessProviderDto>> getProviders(
            @RequestParam(required = false) WellnessProviderType type) {
        return ResponseEntity.ok(wellnessService.getProviders(type));
    }

    // ── Alerts ──

    @GetMapping("/alerts")
    public ResponseEntity<List<AccountabilityAlertDto>> getAlerts(
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(wellnessService.getAlerts(principal.getId()));
    }

    @PutMapping("/alerts/{id}/read")
    public ResponseEntity<Void> markAlertRead(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id) {
        wellnessService.markAlertRead(principal.getId(), id);
        return ResponseEntity.noContent().build();
    }

    // ── Workouts ──

    @GetMapping("/workouts")
    public ResponseEntity<List<WorkoutLogDto>> getWorkouts(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(workoutService.getWorkoutsForDate(principal.getId(), date));
    }

    @GetMapping("/workouts/range")
    public ResponseEntity<List<WorkoutLogDto>> getWorkoutsRange(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end) {
        return ResponseEntity.ok(workoutService.getWorkoutsForRange(principal.getId(), start, end));
    }

    @PostMapping("/workouts")
    public ResponseEntity<WorkoutLogDto> logWorkout(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestBody WorkoutLogDto dto) {
        return ResponseEntity.ok(workoutService.logWorkout(principal.getId(), dto));
    }

    @DeleteMapping("/workouts/{id}")
    public ResponseEntity<Void> deleteWorkout(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id) {
        workoutService.deleteWorkout(principal.getId(), id);
        return ResponseEntity.noContent().build();
    }

    // ── Exercise Catalog ──

    @GetMapping("/exercise/search")
    public ResponseEntity<List<ExerciseCatalogDto>> searchExercises(
            @RequestParam(required = false) String query) {
        return ResponseEntity.ok(workoutService.searchExercises(query));
    }

    @GetMapping("/exercise/category/{category}")
    public ResponseEntity<List<ExerciseCatalogDto>> getExercisesByCategory(
            @PathVariable String category) {
        return ResponseEntity.ok(workoutService.getExercisesByCategory(category));
    }

    // ── Body Metrics ──

    @GetMapping("/body-metrics")
    public ResponseEntity<List<BodyMetricLogDto>> getBodyMetrics(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(defaultValue = "90") int days) {
        return ResponseEntity.ok(fitnessGoalService.getBodyMetrics(principal.getId(), days));
    }

    @GetMapping("/body-metrics/latest")
    public ResponseEntity<BodyMetricLogDto> getLatestBodyMetric(
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(fitnessGoalService.getLatestBodyMetric(principal.getId()));
    }

    @PostMapping("/body-metrics")
    public ResponseEntity<BodyMetricLogDto> logBodyMetric(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestBody BodyMetricLogDto dto) {
        return ResponseEntity.ok(fitnessGoalService.logBodyMetric(principal.getId(), dto));
    }

    // ── Activity Goals ──

    @GetMapping("/activity-goal")
    public ResponseEntity<ActivityGoalDto> getActivityGoal(
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(fitnessGoalService.getActivityGoal(principal.getId()));
    }

    @PutMapping("/activity-goal")
    public ResponseEntity<ActivityGoalDto> saveActivityGoal(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestBody ActivityGoalDto dto) {
        return ResponseEntity.ok(fitnessGoalService.saveActivityGoal(principal.getId(), dto));
    }

    // ── Streak ──

    @GetMapping("/streak")
    public ResponseEntity<FitnessStreakDto> getStreak(
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(fitnessGoalService.getStreak(principal.getId()));
    }

    // ── Steps ──

    @GetMapping("/steps")
    public ResponseEntity<StepLogDto> getSteps(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(stepService.getStepsForDate(principal.getId(), date != null ? date : LocalDate.now()));
    }

    @GetMapping("/steps/range")
    public ResponseEntity<List<StepLogDto>> getStepsRange(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end) {
        return ResponseEntity.ok(stepService.getStepsForRange(principal.getId(), start, end));
    }

    @PostMapping("/steps")
    public ResponseEntity<StepLogDto> logSteps(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestBody StepLogDto dto) {
        return ResponseEntity.ok(stepService.logSteps(principal.getId(), dto));
    }

    @PostMapping("/steps/sync")
    public ResponseEntity<List<StepLogDto>> syncSteps(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestBody List<StepLogDto> batch) {
        return ResponseEntity.ok(stepService.syncFromHealthConnect(principal.getId(), batch));
    }
}
