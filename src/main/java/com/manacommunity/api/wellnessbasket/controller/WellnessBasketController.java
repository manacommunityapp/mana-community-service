package com.manacommunity.api.wellnessbasket.controller;

import com.manacommunity.api.user.model.AppUser;
import com.manacommunity.api.user.security.UserPrincipal;
import com.manacommunity.api.user.service.LoggedInUserService;
import com.manacommunity.api.wellnessbasket.dto.*;
import com.manacommunity.api.wellnessbasket.service.WellnessClusterService;
import com.manacommunity.api.wellnessbasket.service.WellnessMenuRotationService;
import com.manacommunity.api.wellnessbasket.service.WellnessSubscriptionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping({"/api/v1/wellness-basket", "/wellness-basket"})
@RequiredArgsConstructor
public class WellnessBasketController {

    private final WellnessSubscriptionService subscriptionService;
    private final WellnessMenuRotationService menuRotationService;
    private final WellnessClusterService clusterService;
    private final LoggedInUserService loggedInUserService;

    @GetMapping("/plans")
    public ResponseEntity<List<WellnessPlanDto>> getAvailablePlans() {
        return ResponseEntity.ok(subscriptionService.getAvailablePlans());
    }

    @PostMapping("/subscribe")
    public ResponseEntity<WellnessSubscriberDto> subscribe(
            @Valid @RequestBody WellnessSubscribeRequest request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(subscriptionService.subscribe(user.getId(), request));
    }

    @GetMapping("/subscription")
    public ResponseEntity<WellnessSubscriberDto> getMySubscription(
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(subscriptionService.getSubscription(user.getId()));
    }

    @PostMapping("/subscription/pause")
    public ResponseEntity<WellnessSubscriberDto> pauseSubscription(
            @Valid @RequestBody WellnessPauseRequest request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(subscriptionService.pauseSubscription(user.getId(), request));
    }

    @PostMapping("/subscription/resume")
    public ResponseEntity<WellnessSubscriberDto> resumeSubscription(
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(subscriptionService.resumeSubscription(user.getId()));
    }

    @DeleteMapping("/subscription")
    public ResponseEntity<Map<String, String>> cancelSubscription(
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        AppUser user = loggedInUserService.resolve(principal);
        subscriptionService.cancelSubscription(user.getId());
        return ResponseEntity.ok(Map.of("message", "Wellness subscription cancelled successfully"));
    }

    @GetMapping("/menu/week")
    public ResponseEntity<WellnessMenuWeekDto> getMenuForWeek(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate weekStartDate,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        Long userId = null;
        if (principal != null) {
            try {
                AppUser user = loggedInUserService.resolve(principal);
                userId = user.getId();
            } catch (Exception ignored) {
            }
        }
        return ResponseEntity.ok(menuRotationService.getMenuForWeek(userId, weekStartDate));
    }

    @GetMapping("/menu/swap-window")
    public ResponseEntity<Map<String, Object>> getSwapWindowStatus() {
        boolean open = menuRotationService.isSwapWindowOpen();
        String message = menuRotationService.getSwapWindowStatusMessage();
        return ResponseEntity.ok(Map.of(
                "isOpen", open,
                "message", message,
                "window", "Friday 18:00 IST - Saturday 20:00 IST"
        ));
    }

    @PostMapping("/menu/swap")
    public ResponseEntity<WellnessSwapDto> requestSwap(
            @Valid @RequestBody WellnessSwapRequest request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(menuRotationService.requestSwap(user.getId(), request));
    }

    @GetMapping("/menu/swaps")
    public ResponseEntity<List<WellnessSwapDto>> getMySwaps(
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(menuRotationService.getUserSwaps(user.getId()));
    }

    @GetMapping("/clusters")
    public ResponseEntity<List<WellnessClusterDto>> getAllClusters() {
        return ResponseEntity.ok(clusterService.getAllClusters());
    }

    @GetMapping("/cluster/{societyName}")
    public ResponseEntity<WellnessClusterDto> getClusterBySociety(
            @PathVariable String societyName
    ) {
        return clusterService.getClusterBySociety(societyName)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/nutrition/{productId}")
    public ResponseEntity<WellnessNutritionDto> getNutritionForProduct(
            @PathVariable Long productId
    ) {
        return ResponseEntity.ok(menuRotationService.getNutritionForProduct(productId));
    }
}
