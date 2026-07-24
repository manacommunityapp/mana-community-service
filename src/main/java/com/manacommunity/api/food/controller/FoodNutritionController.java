package com.manacommunity.api.food.controller;

import com.manacommunity.api.user.model.AppUser;
import com.manacommunity.api.user.security.UserPrincipal;
import com.manacommunity.api.user.service.LoggedInUserService;
import com.manacommunity.api.food.service.FoodNutritionService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Map;

@RestController
@RequestMapping("/api/food/nutrition")
@RequiredArgsConstructor
public class FoodNutritionController {

    private final FoodNutritionService nutritionService;
    private final LoggedInUserService loggedInUserService;

    @GetMapping("/nutritionists")
    @PreAuthorize("hasAuthority('View Food Nutrition')")
    public ResponseEntity<?> list(
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        Long communityId = user.getCommunity().getId();
        return ResponseEntity.ok(nutritionService.list(communityId));
    }

    @GetMapping("/nutritionists/{id}")
    @PreAuthorize("hasAuthority('View Food Nutrition')")
    public ResponseEntity<?> getNutritionistById(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        Long communityId = user.getCommunity().getId();
        return ResponseEntity.ok(nutritionService.getById(communityId, id));
    }

    @PostMapping("/nutritionists/register")
    @PreAuthorize("hasAuthority('Manage Food Nutrition')")
    public ResponseEntity<?> register(
            @Valid @RequestBody Map<String, Object> request,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        Long communityId = user.getCommunity().getId();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(nutritionService.register(communityId, request));
    }

    @PostMapping("/consultations")
    @PreAuthorize("hasAuthority('Manage Food Nutrition')")
    public ResponseEntity<?> book(
            @Valid @RequestBody Map<String, Object> request,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        Long communityId = user.getCommunity().getId();
        return ResponseEntity.ok(nutritionService.book(communityId, request));
    }

    @GetMapping("/consultations")
    @PreAuthorize("hasAuthority('View Food Nutrition')")
    public ResponseEntity<?> getMyConsultations(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        Long communityId = user.getCommunity().getId();
        PageRequest pageable = PageRequest.of(page, Math.min(size, 50), Sort.by(Sort.Direction.DESC, "createdAt"));
        return ResponseEntity.ok(nutritionService.getMyConsultations(communityId, user.getId(), pageable));
    }

    @PostMapping("/meal-plans")
    @PreAuthorize("hasAuthority('Manage Food Nutrition')")
    public ResponseEntity<?> createMealPlan(
            @Valid @RequestBody Map<String, Object> request,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        Long communityId = user.getCommunity().getId();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(nutritionService.create(communityId, request));
    }

    @GetMapping("/meal-plans")
    @PreAuthorize("hasAuthority('View Food Nutrition')")
    public ResponseEntity<?> getMyMealPlans(
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        Long communityId = user.getCommunity().getId();
        return ResponseEntity.ok(nutritionService.getMyMealPlans(communityId, user.getId()));
    }

    @GetMapping("/meal-plans/{id}")
    @PreAuthorize("hasAuthority('View Food Nutrition')")
    public ResponseEntity<?> getMealPlanById(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        Long communityId = user.getCommunity().getId();
        return ResponseEntity.ok(nutritionService.getById(communityId, id));
    }

    @PostMapping("/calorie-log")
    @PreAuthorize("hasAuthority('Manage Food Nutrition')")
    public ResponseEntity<?> logCalorie(
            @Valid @RequestBody Map<String, Object> request,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        Long communityId = user.getCommunity().getId();
        return ResponseEntity.ok(nutritionService.logCalorie(communityId, user.getId(), request));
    }

    @GetMapping("/calorie-log")
    @PreAuthorize("hasAuthority('View Food Nutrition')")
    public ResponseEntity<?> getCalorieLogByDate(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        Long communityId = user.getCommunity().getId();
        return ResponseEntity.ok(nutritionService.getByDate(communityId, user.getId(), date));
    }

    @GetMapping("/daily-summary")
    @PreAuthorize("hasAuthority('View Food Nutrition')")
    public ResponseEntity<?> getDailyNutrition(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        Long communityId = user.getCommunity().getId();
        return ResponseEntity.ok(nutritionService.getDailyNutrition(communityId, user.getId(), date));
    }

    @PostMapping("/weight-log")
    @PreAuthorize("hasAuthority('Manage Food Nutrition')")
    public ResponseEntity<?> logWeight(
            @Valid @RequestBody Map<String, Object> request,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        Long communityId = user.getCommunity().getId();
        return ResponseEntity.ok(nutritionService.logWeight(communityId, user.getId(), request));
    }

    @GetMapping("/weight-log")
    @PreAuthorize("hasAuthority('View Food Nutrition')")
    public ResponseEntity<?> getWeightHistory(
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        Long communityId = user.getCommunity().getId();
        return ResponseEntity.ok(nutritionService.getHistory(communityId, user.getId()));
    }

    @PostMapping("/water-log")
    @PreAuthorize("hasAuthority('Manage Food Nutrition')")
    public ResponseEntity<?> logWater(
            @Valid @RequestBody Map<String, Object> request,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        Long communityId = user.getCommunity().getId();
        return ResponseEntity.ok(nutritionService.logWater(communityId, user.getId(), request));
    }
}
