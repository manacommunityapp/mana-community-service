package com.manacommunity.api.commerce.core.controller;

import com.manacommunity.api.commerce.core.dto.*;
import com.manacommunity.api.commerce.core.model.CommerceChannel;
import com.manacommunity.api.commerce.core.model.CommerceProduct;
import com.manacommunity.api.commerce.core.model.CommerceRefund;
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
import java.util.Map;

@RestController
@RequestMapping("/api/commerce")
@RequiredArgsConstructor
public class CommerceCoreController {

    private final CommerceCoreService commerceService;
    private final LoggedInUserService loggedInUserService;

    @GetMapping("/products")
    public ResponseEntity<List<CommerceProduct>> getProducts(
            @RequestParam(required = false) CommerceChannel channel) {
        return ResponseEntity.ok(commerceService.getProducts(channel));
    }

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

    @PostMapping("/orders/{orderNumber}/refund")
    public ResponseEntity<CommerceRefund> initiateRefund(
            @PathVariable String orderNumber,
            @RequestBody Map<String, String> body,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        String reason = body.getOrDefault("reason", "Customer requested cancellation");
        return ResponseEntity.ok(commerceService.processRefund(user, orderNumber, reason));
    }

    @GetMapping("/settlements/my")
    public ResponseEntity<List<CommerceSettlementDto>> getMySettlements(
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(commerceService.getMySettlements(user));
    }
}
