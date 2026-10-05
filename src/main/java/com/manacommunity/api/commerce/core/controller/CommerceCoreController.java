package com.manacommunity.api.commerce.core.controller;

import com.manacommunity.api.commerce.core.dto.*;
import com.manacommunity.api.commerce.core.service.CommerceCoreService;
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
@RequestMapping("/api/commerce")
@RequiredArgsConstructor
public class CommerceCoreController {

    private final CommerceCoreService commerceService;
    private final LoggedInUserService loggedInUserService;

    @PostMapping("/orders/checkout")
    public ResponseEntity<CommerceOrderDto> checkout(
            @Valid @RequestBody CommerceCheckoutRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(commerceService.checkout(user, request));
    }

    @GetMapping("/orders/my")
    public ResponseEntity<List<CommerceOrderDto>> getMyOrders(@AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(commerceService.getMyOrders(user));
    }

    @GetMapping("/orders/{orderNumber}")
    public ResponseEntity<CommerceOrderDto> getOrderByNumber(@PathVariable String orderNumber) {
        return ResponseEntity.ok(commerceService.getOrderByNumber(orderNumber));
    }

    @PostMapping("/handover/verify")
    public ResponseEntity<HandoverVerificationResponse> verifyHandover(
            @Valid @RequestBody HandoverVerificationRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(commerceService.verifyHandover(user, request));
    }

    @PostMapping("/reviews")
    public ResponseEntity<CommerceReviewDto> submitReview(
            @Valid @RequestBody CommerceReviewDto dto,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(commerceService.submitReview(user, dto));
    }

    @PostMapping("/disputes")
    public ResponseEntity<CommerceDisputeDto> raiseDispute(
            @Valid @RequestBody CommerceDisputeDto dto,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(commerceService.raiseDispute(user, dto));
    }

    @GetMapping("/settlements/vendor/{vendorId}")
    public ResponseEntity<List<CommerceSettlementDto>> getVendorSettlements(@PathVariable String vendorId) {
        return ResponseEntity.ok(commerceService.getVendorSettlements(vendorId));
    }
}