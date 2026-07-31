package com.manacommunity.api.food.controller;

import com.manacommunity.api.food.service.FoodOrderService;
import com.manacommunity.api.user.model.AppUser;
import com.manacommunity.api.user.security.UserPrincipal;
import com.manacommunity.api.user.service.LoggedInUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/food/orders")
@RequiredArgsConstructor
public class FoodOrderController {

    private final FoodOrderService orderService;
    private final LoggedInUserService loggedInUserService;

    @PostMapping
    @PreAuthorize("hasAuthority('Manage Food Orders')")
    public ResponseEntity<Map<String, Object>> placeOrder(
            @RequestBody Map<String, Object> request,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        Long communityId = user.getCommunity().getId();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(orderService.placeOrder(communityId, request, user));
    }

    @GetMapping
    @PreAuthorize("hasAuthority('View Food Orders')")
    public ResponseEntity<Page<Map<String, Object>>> getMyOrders(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        Long communityId = user.getCommunity().getId();
        PageRequest pageable = PageRequest.of(page, Math.min(size, 50), Sort.by(Sort.Direction.DESC, "createdAt"));
        return ResponseEntity.ok(orderService.getMyOrders(communityId, user.getId(), pageable));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('View Food Orders')")
    public ResponseEntity<Map<String, Object>> getOrderById(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        Long communityId = user.getCommunity().getId();
        return ResponseEntity.ok(orderService.getOrderById(communityId, user.getId(), id));
    }

    @GetMapping("/number/{orderNumber}")
    @PreAuthorize("hasAuthority('View Food Orders')")
    public ResponseEntity<Map<String, Object>> getByNumber(
            @PathVariable String orderNumber,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        Long communityId = user.getCommunity().getId();
        return ResponseEntity.ok(orderService.getByNumber(communityId, user.getId(), orderNumber));
    }

    @GetMapping("/provider")
    @PreAuthorize("hasAuthority('View Food Orders')")
    public ResponseEntity<Page<Map<String, Object>>> getProviderOrders(
            @RequestParam String providerType,
            @RequestParam Long providerId,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        Long communityId = user.getCommunity().getId();
        PageRequest pageable = PageRequest.of(page, Math.min(size, 50), Sort.by(Sort.Direction.DESC, "createdAt"));
        return ResponseEntity.ok(orderService.getProviderOrders(communityId, providerType, providerId, status, pageable));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAuthority('Manage Food Orders')")
    public ResponseEntity<Map<String, Object>> updateStatus(
            @PathVariable Long id,
            @RequestParam String status,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        Long communityId = user.getCommunity().getId();
        return ResponseEntity.ok(orderService.updateStatus(communityId, user.getId(), id, status));
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasAuthority('Manage Food Orders')")
    public ResponseEntity<Map<String, Object>> cancelOrder(
            @PathVariable Long id,
            @RequestBody Map<String, Object> request,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        Long communityId = user.getCommunity().getId();
        return ResponseEntity.ok(orderService.cancelOrder(communityId, user.getId(), id, request));
    }

    @PostMapping("/{id}/rate")
    @PreAuthorize("hasAuthority('Manage Food Orders')")
    public ResponseEntity<Map<String, Object>> rateOrder(
            @PathVariable Long id,
            @RequestBody Map<String, Object> request,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        Long communityId = user.getCommunity().getId();
        return ResponseEntity.ok(orderService.rateOrder(communityId, id, request, user));
    }

    @GetMapping("/{id}/tracking")
    @PreAuthorize("hasAuthority('View Food Orders')")
    public ResponseEntity<Map<String, Object>> getTracking(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        Long communityId = user.getCommunity().getId();
        return ResponseEntity.ok(orderService.getTracking(communityId, user.getId(), id));
    }

    @PostMapping("/group")
    @PreAuthorize("hasAuthority('Manage Food Orders')")
    public ResponseEntity<Map<String, Object>> createGroupOrder(
            @RequestBody Map<String, Object> request,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        Long communityId = user.getCommunity().getId();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(orderService.createGroupOrder(communityId, request, user));
    }

    @PostMapping("/group/join")
    @PreAuthorize("hasAuthority('Manage Food Orders')")
    public ResponseEntity<Map<String, Object>> joinGroupOrder(
            @RequestParam String joinCode,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        Long communityId = user.getCommunity().getId();
        return ResponseEntity.ok(orderService.joinGroupOrder(communityId, joinCode, user));
    }

    @PostMapping("/{id}/refund")
    @PreAuthorize("hasAuthority('Manage Food Orders')")
    public ResponseEntity<Map<String, Object>> requestRefund(
            @PathVariable Long id,
            @RequestBody Map<String, Object> request,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        Long communityId = user.getCommunity().getId();
        return ResponseEntity.ok(orderService.requestRefund(communityId, user.getId(), id, request));
    }
}
