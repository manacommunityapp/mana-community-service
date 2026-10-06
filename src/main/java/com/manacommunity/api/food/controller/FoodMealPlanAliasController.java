package com.manacommunity.api.food.controller;

import com.manacommunity.api.food.service.FoodSubscriptionService;
import com.manacommunity.api.user.model.AppUser;
import com.manacommunity.api.user.security.UserPrincipal;
import com.manacommunity.api.user.service.LoggedInUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/food/meal-plans")
@RequiredArgsConstructor
public class FoodMealPlanAliasController {

    private final FoodSubscriptionService subscriptionService;
    private final LoggedInUserService loggedInUserService;

    @GetMapping
    @PreAuthorize("hasAuthority('View Food Subscriptions')")
    public ResponseEntity<?> getMealPlans(
            @RequestParam(required = false) String targetAudience,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        Long communityId = user.getCommunity().getId();
        return ResponseEntity.ok(subscriptionService.getPlans(communityId, targetAudience));
    }
}
