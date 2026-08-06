package com.manacommunity.api.food.controller;

import com.manacommunity.api.user.model.AppUser;
import com.manacommunity.api.user.security.UserPrincipal;
import com.manacommunity.api.user.service.LoggedInUserService;
import com.manacommunity.api.food.service.FoodDeliveryService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;

@RestController
@RequestMapping("/api/food/delivery")
@RequiredArgsConstructor
public class FoodDeliveryController {

    private final FoodDeliveryService deliveryService;
    private final LoggedInUserService loggedInUserService;

    @PostMapping("/partners/register")
    @PreAuthorize("hasAuthority('Manage Food Delivery')")
    public ResponseEntity<?> register(@AuthenticationPrincipal UserPrincipal principal,
                                      @RequestBody Map<String, Object> request) {
        AppUser user = loggedInUserService.resolve(principal);
        Long communityId = user.getCommunity().getId();
        return ResponseEntity.status(HttpStatus.CREATED).body(deliveryService.register(communityId, request, user));
    }

    @GetMapping("/partners/available")
    @PreAuthorize("hasAuthority('View Food Delivery')")
    public ResponseEntity<?> getAvailable(@AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        Long communityId = user.getCommunity().getId();
        return ResponseEntity.ok(deliveryService.getAvailable(communityId));
    }

    @PostMapping("/assign")
    @PreAuthorize("hasAuthority('Manage Food Delivery')")
    public ResponseEntity<?> assignDelivery(@AuthenticationPrincipal UserPrincipal principal,
                                            @RequestBody Map<String, Object> request) {
        AppUser user = loggedInUserService.resolve(principal);
        Long communityId = user.getCommunity().getId();
        return ResponseEntity.ok(deliveryService.assignDelivery(communityId, request));
    }

    @PatchMapping("/assignments/{id}/status")
    @PreAuthorize("hasAuthority('Manage Food Delivery')")
    public ResponseEntity<?> updateStatus(@AuthenticationPrincipal UserPrincipal principal,
                                          @PathVariable Long id,
                                          @RequestParam String status,
                                          @RequestParam(required = false) BigDecimal latitude,
                                          @RequestParam(required = false) BigDecimal longitude) {
        AppUser user = loggedInUserService.resolve(principal);
        Long communityId = user.getCommunity().getId();
        return ResponseEntity.ok(deliveryService.updateStatus(communityId, id, status, latitude, longitude));
    }

    @PostMapping("/assignments/{id}/verify-otp")
    @PreAuthorize("hasAuthority('Manage Food Delivery')")
    public ResponseEntity<?> verifyOtp(@AuthenticationPrincipal UserPrincipal principal,
                                       @PathVariable Long id,
                                       @RequestParam String otp) {
        AppUser user = loggedInUserService.resolve(principal);
        Long communityId = user.getCommunity().getId();
        return ResponseEntity.ok(deliveryService.verifyOtp(communityId, id, otp));
    }

    @GetMapping("/my-deliveries")
    @PreAuthorize("hasAuthority('View Food Delivery')")
    public ResponseEntity<?> getMyDeliveries(@AuthenticationPrincipal UserPrincipal principal,
                                             @RequestParam(required = false) String status,
                                             @RequestParam(defaultValue = "0") int page,
                                             @RequestParam(defaultValue = "20") int size) {
        AppUser user = loggedInUserService.resolve(principal);
        Long communityId = user.getCommunity().getId();
        return ResponseEntity.ok(deliveryService.getMyDeliveries(communityId, user.getId(), status, PageRequest.of(page, size)));
    }

    @PutMapping("/partners/location")
    @PreAuthorize("hasAuthority('Manage Food Delivery')")
    public ResponseEntity<?> updateLocation(@AuthenticationPrincipal UserPrincipal principal,
                                            @RequestBody Map<String, Object> request) {
        AppUser user = loggedInUserService.resolve(principal);
        Long communityId = user.getCommunity().getId();
        return ResponseEntity.ok(deliveryService.updateLocation(communityId, request));
    }

    @GetMapping("/zones")
    @PreAuthorize("hasAuthority('View Food Delivery')")
    public ResponseEntity<?> getZones(@AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        Long communityId = user.getCommunity().getId();
        return ResponseEntity.ok(deliveryService.getZones(communityId));
    }
}
