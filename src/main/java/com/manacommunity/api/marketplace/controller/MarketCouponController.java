package com.manacommunity.api.marketplace.controller;

import com.manacommunity.api.marketplace.dto.MarketCouponDto;
import com.manacommunity.api.marketplace.service.MarketCouponService;
import com.manacommunity.api.user.model.AppUser;
import com.manacommunity.api.user.security.UserPrincipal;
import com.manacommunity.api.user.service.LoggedInUserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping({"/api/marketplace/coupons", "/api/v1/marketplace/coupons"})
@RequiredArgsConstructor
public class MarketCouponController {

    private final MarketCouponService couponService;
    private final LoggedInUserService loggedInUserService;

    @PostMapping("/validate")
    @PreAuthorize("hasAuthority('View Marketplace')")
    public ResponseEntity<MarketCouponDto.Response> validateCoupon(
            @Valid @RequestBody MarketCouponDto.ValidateRequest req) {
        return ResponseEntity.ok(couponService.validateCoupon(req.getCode(), req.getSubtotal()));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('Manage Categories')")
    public ResponseEntity<MarketCouponDto.Response> createCoupon(
            @Valid @RequestBody MarketCouponDto.Request req,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(couponService.create(req, user.getCommunity()));
    }
}
