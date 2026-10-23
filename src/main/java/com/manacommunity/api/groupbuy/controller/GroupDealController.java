package com.manacommunity.api.groupbuy.controller;

import com.manacommunity.api.groupbuy.dto.*;
import com.manacommunity.api.groupbuy.service.GroupDealService;
import com.manacommunity.api.user.model.AppUser;
import com.manacommunity.api.user.security.UserPrincipal;
import com.manacommunity.api.user.service.LoggedInUserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/group-buying/deals")
@RequiredArgsConstructor
public class GroupDealController {

    private final GroupDealService dealService;
    private final LoggedInUserService loggedInUserService;

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Page<GroupDealResponse>> getDeals(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "ACTIVE") String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        Long communityId = user.getCommunity() != null ? user.getCommunity().getId() : null;
        if (communityId == null) return ResponseEntity.ok(Page.empty());
        return ResponseEntity.ok(dealService.getDeals(communityId, category, search, status, page, size, user.getId()));
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<GroupDealResponse> getById(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(dealService.getById(id, user.getId()));
    }

    @GetMapping("/almost-unlocked")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<GroupDealResponse>> getAlmostUnlocked(
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        Long communityId = user.getCommunity() != null ? user.getCommunity().getId() : null;
        if (communityId == null) return ResponseEntity.ok(List.of());
        return ResponseEntity.ok(dealService.getAlmostUnlocked(communityId, user.getId()));
    }

    @GetMapping("/ending-soon")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<GroupDealResponse>> getEndingSoon(
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        Long communityId = user.getCommunity() != null ? user.getCommunity().getId() : null;
        if (communityId == null) return ResponseEntity.ok(List.of());
        return ResponseEntity.ok(dealService.getEndingSoon(communityId, user.getId()));
    }

    @GetMapping("/festival")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<GroupDealResponse>> getFestivalDeals(
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        Long communityId = user.getCommunity() != null ? user.getCommunity().getId() : null;
        if (communityId == null) return ResponseEntity.ok(List.of());
        return ResponseEntity.ok(dealService.getFestivalDeals(communityId, user.getId()));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('Admin') or hasAuthority('Manage Deals')")
    public ResponseEntity<GroupDealResponse> createDeal(
            @Valid @RequestBody GroupDealRequest req,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(dealService.create(req, user, user.getCommunity()));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAuthority('Admin') or hasAuthority('Manage Deals')")
    public ResponseEntity<GroupDealResponse> updateStatus(
            @PathVariable Long id,
            @RequestParam String status,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(dealService.updateStatus(id, status, user.getId()));
    }

    @PostMapping("/{id}/join")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<GroupOrderResponse> joinDeal(
            @PathVariable Long id,
            @Valid @RequestBody JoinDealRequest req,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(dealService.joinDeal(id, req, user, user.getCommunity()));
    }

    @GetMapping("/my-orders")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<GroupOrderResponse>> getMyOrders(
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(dealService.getMyOrders(user.getId()));
    }

    @DeleteMapping("/orders/{orderId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> cancelOrder(
            @PathVariable Long orderId,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        dealService.cancelOrder(orderId, user.getId());
        return ResponseEntity.noContent().build();
    }
}
