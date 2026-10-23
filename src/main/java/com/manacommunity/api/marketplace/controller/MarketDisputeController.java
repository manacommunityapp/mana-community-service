package com.manacommunity.api.marketplace.controller;

import com.manacommunity.api.marketplace.dto.MarketDisputeRequest;
import com.manacommunity.api.marketplace.dto.MarketDisputeResponse;
import com.manacommunity.api.marketplace.entity.MarketDispute;
import com.manacommunity.api.marketplace.service.MarketDisputeService;
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

import java.util.List;

@RestController
@RequestMapping({"/api/marketplace/disputes", "/api/v1/marketplace/disputes"})
@RequiredArgsConstructor
public class MarketDisputeController {

    private final MarketDisputeService disputeService;
    private final LoggedInUserService loggedInUserService;

    @GetMapping
    @PreAuthorize("hasAuthority('Manage Categories')")
    public ResponseEntity<Page<MarketDisputeResponse>> getDisputes(
            @RequestParam(required = false) MarketDispute.DisputeStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        Long communityId = user.getCommunity() != null ? user.getCommunity().getId() : null;
        if (communityId == null) return ResponseEntity.ok(Page.empty());
        PageRequest pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return ResponseEntity.ok(disputeService.getCommunityDisputes(communityId, status, pageable));
    }

    @GetMapping("/mine")
    @PreAuthorize("hasAuthority('View Marketplace')")
    public ResponseEntity<List<MarketDisputeResponse>> getMyDisputes(@AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(disputeService.getMyDisputes(user.getId()));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('View Marketplace')")
    public ResponseEntity<MarketDisputeResponse> raiseDispute(
            @Valid @RequestBody MarketDisputeRequest req,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(disputeService.raiseDispute(req, user, user.getCommunity()));
    }

    @PutMapping("/{id}/resolve")
    @PreAuthorize("hasAuthority('Manage Categories')")
    public ResponseEntity<MarketDisputeResponse> resolveDispute(
            @PathVariable Long id,
            @Valid @RequestBody MarketDisputeRequest.ResolveRequest req,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser admin = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(disputeService.resolveDispute(id, req, admin));
    }
}
