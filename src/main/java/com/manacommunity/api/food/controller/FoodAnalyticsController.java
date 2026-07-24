package com.manacommunity.api.food.controller;

import com.manacommunity.api.user.model.AppUser;
import com.manacommunity.api.user.security.UserPrincipal;
import com.manacommunity.api.user.service.LoggedInUserService;
import com.manacommunity.api.food.service.FoodAnalyticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/food/analytics")
@RequiredArgsConstructor
public class FoodAnalyticsController {

    private final FoodAnalyticsService analyticsService;
    private final LoggedInUserService loggedInUserService;

    @GetMapping("/restaurant/{restaurantId}")
    @PreAuthorize("hasAuthority('View Food Analytics')")
    public ResponseEntity<?> getRestaurantAnalytics(@AuthenticationPrincipal UserPrincipal principal,
                                                     @PathVariable Long restaurantId,
                                                     @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
                                                     @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        AppUser user = loggedInUserService.resolve(principal);
        Long communityId = user.getCommunity().getId();
        return ResponseEntity.ok(analyticsService.getRestaurantAnalytics(communityId, restaurantId, startDate, endDate));
    }

    @GetMapping("/home-chef/{chefId}")
    @PreAuthorize("hasAuthority('View Food Analytics')")
    public ResponseEntity<?> getHomeChefAnalytics(@AuthenticationPrincipal UserPrincipal principal,
                                                   @PathVariable Long chefId,
                                                   @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
                                                   @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        AppUser user = loggedInUserService.resolve(principal);
        Long communityId = user.getCommunity().getId();
        return ResponseEntity.ok(analyticsService.getHomeChefAnalytics(communityId, chefId, startDate, endDate));
    }

    @GetMapping("/community-trends")
    @PreAuthorize("hasAuthority('View Food Analytics')")
    public ResponseEntity<?> getCommunityTrends(@AuthenticationPrincipal UserPrincipal principal,
                                                 @RequestParam(defaultValue = "6") int months) {
        AppUser user = loggedInUserService.resolve(principal);
        Long communityId = user.getCommunity().getId();
        return ResponseEntity.ok(analyticsService.getCommunityTrends(communityId, months));
    }

    @GetMapping("/food-waste")
    @PreAuthorize("hasAuthority('View Food Analytics')")
    public ResponseEntity<?> getFoodWaste(@AuthenticationPrincipal UserPrincipal principal,
                                           @RequestParam(defaultValue = "6") int months) {
        AppUser user = loggedInUserService.resolve(principal);
        Long communityId = user.getCommunity().getId();
        return ResponseEntity.ok(analyticsService.getFoodWaste(communityId, months));
    }

    @GetMapping("/revenue")
    @PreAuthorize("hasAuthority('View Food Analytics')")
    public ResponseEntity<?> getRevenue(@AuthenticationPrincipal UserPrincipal principal,
                                         @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
                                         @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        AppUser user = loggedInUserService.resolve(principal);
        Long communityId = user.getCommunity().getId();
        return ResponseEntity.ok(analyticsService.getRevenue(communityId, startDate, endDate));
    }

    @GetMapping("/dashboard")
    @PreAuthorize("hasAuthority('View Food Analytics')")
    public ResponseEntity<?> getDashboardStats(@AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        Long communityId = user.getCommunity().getId();
        return ResponseEntity.ok(analyticsService.getDashboardStats(communityId));
    }
}
