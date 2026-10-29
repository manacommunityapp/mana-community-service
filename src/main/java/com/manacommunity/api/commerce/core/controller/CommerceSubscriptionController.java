package com.manacommunity.api.commerce.core.controller;

import com.manacommunity.api.commerce.core.dto.CommerceSubscriptionDto;
import com.manacommunity.api.commerce.core.dto.CommerceSubscriptionRequest;
import com.manacommunity.api.commerce.core.service.CommerceSubscriptionService;
import com.manacommunity.api.user.model.AppUser;
import com.manacommunity.api.user.security.UserPrincipal;
import com.manacommunity.api.user.service.LoggedInUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/commerce/subscriptions")
@RequiredArgsConstructor
public class CommerceSubscriptionController {

    private final CommerceSubscriptionService subscriptionService;
    private final LoggedInUserService loggedInUserService;

    @PostMapping
    public ResponseEntity<CommerceSubscriptionDto> create(
            @RequestBody CommerceSubscriptionRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(subscriptionService.createSubscription(user, request));
    }

    @GetMapping("/my")
    public ResponseEntity<List<CommerceSubscriptionDto>> getMy(
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(subscriptionService.getMySubscriptions(user));
    }

    @PostMapping("/{code}/pause")
    public ResponseEntity<CommerceSubscriptionDto> pause(
            @PathVariable String code,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(subscriptionService.pauseSubscription(user, code));
    }

    @PostMapping("/{code}/resume")
    public ResponseEntity<CommerceSubscriptionDto> resume(
            @PathVariable String code,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(subscriptionService.resumeSubscription(user, code));
    }

    @PostMapping("/{code}/cancel")
    public ResponseEntity<CommerceSubscriptionDto> cancel(
            @PathVariable String code,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(subscriptionService.cancelSubscription(user, code));
    }

    @GetMapping("/admin")
    @PreAuthorize("hasAuthority('Manage Commerce Orders')")
    public ResponseEntity<List<CommerceSubscriptionDto>> getAdminAll(
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(subscriptionService.getAdminSubscriptions(user));
    }
}
