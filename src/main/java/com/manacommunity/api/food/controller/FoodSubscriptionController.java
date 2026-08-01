package com.manacommunity.api.food.controller;

import com.manacommunity.api.user.model.AppUser;
import com.manacommunity.api.user.security.UserPrincipal;
import com.manacommunity.api.user.service.LoggedInUserService;
import com.manacommunity.api.food.service.FoodSubscriptionService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/food/subscriptions")
@RequiredArgsConstructor
public class FoodSubscriptionController {

    private final FoodSubscriptionService subscriptionService;
    private final LoggedInUserService loggedInUserService;

    @GetMapping("/plans")
    @PreAuthorize("hasAuthority('View Food Subscriptions')")
    public ResponseEntity<?> getPlans(
            @RequestParam(required = false) String targetAudience,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        Long communityId = user.getCommunity().getId();
        return ResponseEntity.ok(subscriptionService.getPlans(communityId, targetAudience));
    }

    @GetMapping("/plans/{id}")
    @PreAuthorize("hasAuthority('View Food Subscriptions')")
    public ResponseEntity<?> getPlanById(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        Long communityId = user.getCommunity().getId();
        return ResponseEntity.ok(subscriptionService.getPlanById(communityId, id));
    }

    @PostMapping("/plans")
    @PreAuthorize("hasAuthority('Manage Food Subscriptions')")
    public ResponseEntity<?> createPlan(
            @Valid @RequestBody Map<String, Object> request,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        Long communityId = user.getCommunity().getId();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(subscriptionService.createPlan(communityId, request));
    }

    @PutMapping("/plans/{id}")
    @PreAuthorize("hasAuthority('Manage Food Subscriptions')")
    public ResponseEntity<?> updatePlan(
            @PathVariable Long id,
            @Valid @RequestBody Map<String, Object> request,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        Long communityId = user.getCommunity().getId();
        return ResponseEntity.ok(subscriptionService.updatePlan(communityId, id, request));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('Manage Food Subscriptions')")
    public ResponseEntity<?> subscribe(
            @Valid @RequestBody Map<String, Object> request,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        Long communityId = user.getCommunity().getId();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(subscriptionService.subscribe(communityId, request, user));
    }

    @GetMapping("/my")
    @PreAuthorize("hasAuthority('View Food Subscriptions')")
    public ResponseEntity<?> getMySubscriptions(
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        Long communityId = user.getCommunity().getId();
        return ResponseEntity.ok(subscriptionService.getMySubscriptions(communityId, user.getId()));
    }

    @PostMapping("/{id}/pause")
    @PreAuthorize("hasAuthority('Manage Food Subscriptions')")
    public ResponseEntity<?> pause(
            @PathVariable Long id,
            @Valid @RequestBody Map<String, Object> request,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        Long communityId = user.getCommunity().getId();
        return ResponseEntity.ok(subscriptionService.pause(communityId, id, request));
    }

    @PostMapping("/{id}/resume")
    @PreAuthorize("hasAuthority('Manage Food Subscriptions')")
    public ResponseEntity<?> resume(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        Long communityId = user.getCommunity().getId();
        return ResponseEntity.ok(subscriptionService.resume(communityId, id));
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasAuthority('Manage Food Subscriptions')")
    public ResponseEntity<?> cancel(
            @PathVariable Long id,
            @Valid @RequestBody Map<String, Object> request,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        Long communityId = user.getCommunity().getId();
        return ResponseEntity.ok(subscriptionService.cancel(communityId, id, request));
    }

    @GetMapping("/{id}/deliveries")
    @PreAuthorize("hasAuthority('View Food Subscriptions')")
    public ResponseEntity<?> getDeliveries(
            @PathVariable Long id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        Long communityId = user.getCommunity().getId();
        PageRequest pageable = PageRequest.of(page, Math.min(size, 50), Sort.by(Sort.Direction.DESC, "createdAt"));
        return ResponseEntity.ok(subscriptionService.getDeliveries(communityId, id, pageable));
    }
}
