package com.manacommunity.api.food.controller;

import com.manacommunity.api.user.model.AppUser;
import com.manacommunity.api.user.security.UserPrincipal;
import com.manacommunity.api.user.service.LoggedInUserService;
import com.manacommunity.api.food.service.FoodLoyaltyService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

@RestController
@RequestMapping("/api/food/loyalty")
@RequiredArgsConstructor
public class FoodLoyaltyController {

    private final FoodLoyaltyService loyaltyService;
    private final LoggedInUserService loggedInUserService;

    @GetMapping("/programs")
    @PreAuthorize("hasAuthority('View Food Loyalty')")
    public ResponseEntity<?> list(@AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        Long communityId = user.getCommunity().getId();
        return ResponseEntity.ok(loyaltyService.list(communityId));
    }

    @PostMapping("/enroll")
    @PreAuthorize("hasAuthority('Manage Food Loyalty')")
    public ResponseEntity<?> enroll(@AuthenticationPrincipal UserPrincipal principal,
                                    @RequestBody Map<String, Object> request) {
        AppUser user = loggedInUserService.resolve(principal);
        Long communityId = user.getCommunity().getId();
        Long programId = Long.valueOf(request.get("programId").toString());
        return ResponseEntity.ok(loyaltyService.enroll(communityId, programId, user));
    }

    @GetMapping("/my-info")
    @PreAuthorize("hasAuthority('View Food Loyalty')")
    public ResponseEntity<?> getMemberInfo(@AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        Long communityId = user.getCommunity().getId();
        return ResponseEntity.ok(loyaltyService.getMemberInfo(communityId, user.getId()));
    }

    @PostMapping("/earn")
    @PreAuthorize("hasAuthority('Manage Food Loyalty')")
    public ResponseEntity<?> earnPoints(@AuthenticationPrincipal UserPrincipal principal,
                                        @RequestBody Map<String, Object> request) {
        AppUser user = loggedInUserService.resolve(principal);
        Long communityId = user.getCommunity().getId();
        Long userId = Long.valueOf(request.get("userId").toString());
        Integer points = Integer.valueOf(request.get("points").toString());
        String referenceType = (String) request.get("referenceType");
        Long referenceId = Long.valueOf(request.get("referenceId").toString());
        return ResponseEntity.ok(loyaltyService.earnPoints(communityId, userId, points, referenceType, referenceId));
    }

    @PostMapping("/redeem")
    @PreAuthorize("hasAuthority('Manage Food Loyalty')")
    public ResponseEntity<?> redeemPoints(@AuthenticationPrincipal UserPrincipal principal,
                                          @RequestBody Map<String, Object> request) {
        AppUser user = loggedInUserService.resolve(principal);
        Long communityId = user.getCommunity().getId();
        Integer points = Integer.valueOf(request.get("points").toString());
        return ResponseEntity.ok(loyaltyService.redeemPoints(communityId, user.getId(), points));
    }

    @PostMapping("/coupons/validate")
    @PreAuthorize("hasAuthority('View Food Loyalty')")
    public ResponseEntity<?> validateCoupon(@AuthenticationPrincipal UserPrincipal principal,
                                            @RequestBody Map<String, Object> request) {
        AppUser user = loggedInUserService.resolve(principal);
        Long communityId = user.getCommunity().getId();
        String code = (String) request.get("code");
        BigDecimal orderAmount = new BigDecimal(request.get("orderAmount").toString());
        return ResponseEntity.ok(loyaltyService.validateCoupon(communityId, code, orderAmount));
    }

    @PostMapping("/coupons/apply")
    @PreAuthorize("hasAuthority('Manage Food Loyalty')")
    public ResponseEntity<?> applyCoupon(@AuthenticationPrincipal UserPrincipal principal,
                                         @RequestBody Map<String, Object> request) {
        AppUser user = loggedInUserService.resolve(principal);
        Long communityId = user.getCommunity().getId();
        String code = (String) request.get("code");
        Long orderId = Long.valueOf(request.get("orderId").toString());
        BigDecimal discountAmount = new BigDecimal(request.get("discountAmount").toString());
        return ResponseEntity.ok(loyaltyService.applyCoupon(communityId, code, orderId, discountAmount, user));
    }

    @GetMapping("/gift-cards")
    @PreAuthorize("hasAuthority('View Food Loyalty')")
    public ResponseEntity<?> getGiftCards(@AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        Long communityId = user.getCommunity().getId();
        return ResponseEntity.ok(loyaltyService.getGiftCards(communityId, user.getId()));
    }

    @PostMapping("/gift-cards")
    @PreAuthorize("hasAuthority('Manage Food Loyalty')")
    public ResponseEntity<?> purchase(@AuthenticationPrincipal UserPrincipal principal,
                                      @RequestBody Map<String, Object> request) {
        AppUser user = loggedInUserService.resolve(principal);
        Long communityId = user.getCommunity().getId();
        BigDecimal amount = new BigDecimal(request.get("amount").toString());
        Long giftedToUserId = Long.valueOf(request.get("giftedToUserId").toString());
        LocalDate validUntil = LocalDate.parse(request.get("validUntil").toString());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(loyaltyService.purchase(communityId, amount, giftedToUserId, validUntil, user));
    }
}
