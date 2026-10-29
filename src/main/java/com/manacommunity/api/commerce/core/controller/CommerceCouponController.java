package com.manacommunity.api.commerce.core.controller;

import com.manacommunity.api.commerce.core.dto.*;
import com.manacommunity.api.commerce.core.service.CommerceCouponService;
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
@RequestMapping("/api/v1/commerce/coupons")
@RequiredArgsConstructor
public class CommerceCouponController {

    private final CommerceCouponService couponService;
    private final LoggedInUserService loggedInUserService;

    // ── Admin ──────────────────────────────────────────────────────────────────

    @GetMapping("/admin")
    @PreAuthorize("hasAuthority('Manage Commerce Orders')")
    public ResponseEntity<List<CommerceCouponDto>> getAdmin(
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(couponService.getAdminCoupons(loggedInUserService.resolve(principal)));
    }

    @PostMapping("/admin")
    @PreAuthorize("hasAuthority('Manage Commerce Orders')")
    public ResponseEntity<CommerceCouponDto> create(
            @RequestBody CommerceCouponRequest req,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(couponService.createCoupon(loggedInUserService.resolve(principal), req));
    }

    @PutMapping("/admin/{id}")
    @PreAuthorize("hasAuthority('Manage Commerce Orders')")
    public ResponseEntity<CommerceCouponDto> update(
            @PathVariable Long id,
            @RequestBody CommerceCouponRequest req,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(couponService.updateCoupon(loggedInUserService.resolve(principal), id, req));
    }

    @DeleteMapping("/admin/{id}")
    @PreAuthorize("hasAuthority('Manage Commerce Orders')")
    public ResponseEntity<Void> delete(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        couponService.deleteCoupon(loggedInUserService.resolve(principal), id);
        return ResponseEntity.ok().build();
    }

    // ── Customer ───────────────────────────────────────────────────────────────

    @PostMapping("/validate")
    public ResponseEntity<CouponApplyResult> validate(
            @RequestBody CouponApplyRequest req,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(couponService.validateCoupon(user, req.getCouponCode(), req.getOrderAmount()));
    }
}
