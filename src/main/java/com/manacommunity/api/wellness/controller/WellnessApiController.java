package com.manacommunity.api.wellness.controller;

import com.manacommunity.api.user.security.UserPrincipal;
import com.manacommunity.api.wellness.dto.*;
import com.manacommunity.api.wellness.entity.FoodCatalog;
import com.manacommunity.api.wellness.enums.WellnessEnums.*;
import com.manacommunity.api.wellness.service.WellnessService;
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

    public WellnessApiController(WellnessService wellnessService) {
        this.wellnessService = wellnessService;
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
}
