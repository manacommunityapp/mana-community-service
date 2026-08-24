package com.manacommunity.api.events.controller;

import com.manacommunity.api.events.dto.MealRegistrationRequest;
import com.manacommunity.api.events.dto.MealRegistrationResponse;
import com.manacommunity.api.events.dto.MealSummaryResponse;
import com.manacommunity.api.events.service.EventMealService;
import com.manacommunity.api.user.model.AppUser;
import com.manacommunity.api.user.security.UserPrincipal;
import com.manacommunity.api.user.service.LoggedInUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/events")
@RequiredArgsConstructor
public class EventMealController {

    private final EventMealService mealService;
    private final LoggedInUserService loggedInUserService;

    @GetMapping("/{eventId}/meals")
    @PreAuthorize("hasAnyRole('ADMIN','COMMUNITY_ADMIN','SUPER_ADMIN','EVENT_ADMIN','FOOD_ADMIN','USER','RESIDENT') or hasAuthority('View Events')")
    public ResponseEntity<MealRegistrationResponse> getUserMeals(
            @PathVariable Long eventId,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(mealService.getUserMeals(eventId, user.getId()));
    }

    @PostMapping("/{eventId}/meals")
    @PreAuthorize("hasAnyRole('ADMIN','COMMUNITY_ADMIN','SUPER_ADMIN','EVENT_ADMIN','FOOD_ADMIN','USER','RESIDENT') or hasAuthority('View Events')")
    public ResponseEntity<MealRegistrationResponse> saveMeals(
            @PathVariable Long eventId,
            @RequestBody MealRegistrationRequest req,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(mealService.saveMeals(eventId, req, user));
    }

    @GetMapping("/{eventId}/meals/summary")
    @PreAuthorize("hasAnyRole('ADMIN','COMMUNITY_ADMIN','SUPER_ADMIN','EVENT_ADMIN','FOOD_ADMIN','USER','RESIDENT') or hasAuthority('View Events')")
    public ResponseEntity<MealSummaryResponse> getMealSummary(
            @PathVariable Long eventId) {
        return ResponseEntity.ok(mealService.getMealSummary(eventId));
    }
}
