package com.manacommunity.api.marketplace.controller;

import com.manacommunity.api.marketplace.dto.MarketHandoverPassDto;
import com.manacommunity.api.marketplace.dto.MarketOrderRequest;
import com.manacommunity.api.marketplace.dto.MarketOrderResponse;
import com.manacommunity.api.marketplace.service.MarketHandoverSecurityService;
import com.manacommunity.api.marketplace.service.MarketOrderService;
import com.manacommunity.api.user.model.AppUser;
import com.manacommunity.api.user.security.UserPrincipal;
import com.manacommunity.api.user.service.LoggedInUserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping({"/api/marketplace/orders", "/api/v1/marketplace/orders"})
@RequiredArgsConstructor
public class MarketOrderController {

    private final MarketOrderService orderService;
    private final MarketHandoverSecurityService handoverSecurityService;
    private final LoggedInUserService loggedInUserService;

    @GetMapping("/purchases")
    @PreAuthorize("hasAuthority('View Marketplace')")
    public ResponseEntity<Page<MarketOrderResponse>> getMyPurchases(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        PageRequest pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return ResponseEntity.ok(orderService.getMyPurchases(user.getId(), pageable));
    }

    @GetMapping("/sales")
    @PreAuthorize("hasAuthority('View Marketplace')")
    public ResponseEntity<Page<MarketOrderResponse>> getMySales(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        PageRequest pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return ResponseEntity.ok(orderService.getMySales(user.getId(), pageable));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('View Marketplace')")
    public ResponseEntity<MarketOrderResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(orderService.getById(id));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('View Marketplace')")
    public ResponseEntity<MarketOrderResponse> createOrder(
            @Valid @RequestBody MarketOrderRequest req,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(orderService.createOrder(req, user, user.getCommunity()));
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("hasAuthority('View Marketplace')")
    public ResponseEntity<MarketOrderResponse> updateStatus(
            @PathVariable Long id,
            @RequestParam String status) {
        return ResponseEntity.ok(orderService.updateStatus(id, status));
    }

    @PostMapping("/verify-gate-pass")
    @PreAuthorize("hasAuthority('View Marketplace')")
    public ResponseEntity<MarketHandoverPassDto> verifyGatePass(
            @Valid @RequestBody MarketHandoverPassDto.VerifyRequest req,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(handoverSecurityService.verifyPassAtGate(req, user));
    }
}
