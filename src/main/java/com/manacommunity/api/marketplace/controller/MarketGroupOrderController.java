package com.manacommunity.api.marketplace.controller;

import com.manacommunity.api.marketplace.dto.MarketGroupOrderDto;
import com.manacommunity.api.marketplace.dto.MarketGroupOrderParticipantDto;
import com.manacommunity.api.marketplace.service.MarketGroupOrderService;
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
@RequestMapping({"/api/marketplace/group-orders", "/api/v1/marketplace/group-orders"})
@RequiredArgsConstructor
public class MarketGroupOrderController {

    private final MarketGroupOrderService groupOrderService;
    private final LoggedInUserService loggedInUserService;

    @GetMapping
    @PreAuthorize("hasAuthority('View Marketplace')")
    public ResponseEntity<Page<MarketGroupOrderDto.Response>> getGroupOrders(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        Long communityId = user.getCommunity() != null ? user.getCommunity().getId() : null;
        if (communityId == null) return ResponseEntity.ok(Page.empty());
        PageRequest pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return ResponseEntity.ok(groupOrderService.getActiveGroupOrders(communityId, pageable));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('View Marketplace')")
    public ResponseEntity<MarketGroupOrderDto.Response> getById(@PathVariable Long id) {
        return ResponseEntity.ok(groupOrderService.getById(id));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('Create Listing')")
    public ResponseEntity<MarketGroupOrderDto.Response> create(
            @Valid @RequestBody MarketGroupOrderDto.Request req,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(groupOrderService.create(req, user, user.getCommunity()));
    }

    @PostMapping("/{id}/join")
    @PreAuthorize("hasAuthority('View Marketplace')")
    public ResponseEntity<MarketGroupOrderParticipantDto.Response> join(
            @PathVariable Long id,
            @Valid @RequestBody MarketGroupOrderParticipantDto.JoinRequest req,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(groupOrderService.joinGroupOrder(id, req, user));
    }
}
