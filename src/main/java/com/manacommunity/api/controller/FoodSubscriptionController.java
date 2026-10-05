package com.manacommunity.api.controller;

import com.manacommunity.api.dto.MealPlanResponse;
import com.manacommunity.api.dto.MealSubscriptionRequest;
import com.manacommunity.api.dto.MealSubscriptionResponse;
import com.manacommunity.api.service.MealPlanService;
import com.manacommunity.api.service.MealSubscriptionService;
import com.manacommunity.api.user.model.AppUser;
import com.manacommunity.api.user.security.UserPrincipal;
import com.manacommunity.api.user.service.LoggedInUserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/food")
@RequiredArgsConstructor
public class FoodSubscriptionController {

    private final MealPlanService mealPlanService;
    private final MealSubscriptionService subscriptionService;
    private final LoggedInUserService loggedInUserService;

    // ── Meal Plans ──────────────────────────────────────────────────────────

    @GetMapping("/meal-plans")
    public ResponseEntity<List<MealPlanResponse>> getMealPlans(
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(mealPlanService.getActiveMealPlans(communityId(principal)));
    }

    // ── Subscriptions ───────────────────────────────────────────────────────

    @GetMapping("/subscriptions")
    public ResponseEntity<List<MealSubscriptionResponse>> getSubscriptions(
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = requireMember(principal);
        return ResponseEntity.ok(subscriptionService.getUserSubscriptions(user.getId()));
    }

    @PostMapping("/subscriptions")
    public ResponseEntity<MealSubscriptionResponse> createSubscription(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody MealSubscriptionRequest request) {
        AppUser user = requireMember(principal);
        return ResponseEntity.ok(subscriptionService.createSubscription(request, user));
    }

    @PutMapping("/subscriptions/{id}/pause")
    public ResponseEntity<MealSubscriptionResponse> pauseSubscription(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id) {
        AppUser user = requireMember(principal);
        return ResponseEntity.ok(subscriptionService.pauseSubscription(id, user.getId()));
    }

    @PutMapping("/subscriptions/{id}/resume")
    public ResponseEntity<MealSubscriptionResponse> resumeSubscription(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id) {
        AppUser user = requireMember(principal);
        return ResponseEntity.ok(subscriptionService.resumeSubscription(id, user.getId()));
    }

    @DeleteMapping("/subscriptions/{id}")
    public ResponseEntity<Void> cancelSubscription(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id) {
        AppUser user = requireMember(principal);
        subscriptionService.cancelSubscription(id, user.getId());
        return ResponseEntity.noContent().build();
    }

    // ── Helpers ─────────────────────────────────────────────────────────────

    private Long communityId(UserPrincipal principal) {
        return requireMember(principal).getCommunity().getId();
    }

    private AppUser requireMember(UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        if (user.getCommunity() == null) {
            throw new IllegalArgumentException("Food features are scoped to a community.");
        }
        return user;
    }
}
