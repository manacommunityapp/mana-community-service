package com.manacommunity.api.events.controller;

import com.manacommunity.api.events.dto.EventAuctionBidRequest;
import com.manacommunity.api.events.dto.EventAuctionBidResponse;
import com.manacommunity.api.events.dto.EventAuctionItemRequest;
import com.manacommunity.api.events.dto.EventAuctionItemResponse;
import com.manacommunity.api.events.dto.EventAuctionStatsResponse;
import com.manacommunity.api.events.service.EventAuctionService;
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

import java.util.List;

@RestController
@RequestMapping("/api/events/auction-items")
@RequiredArgsConstructor
public class EventAuctionController {

    private final EventAuctionService auctionService;
    private final LoggedInUserService loggedInUserService;

    @GetMapping
    @PreAuthorize("hasAuthority('View Events')")
    public ResponseEntity<List<EventAuctionItemResponse>> getItems(
            @RequestParam(required = false) Long eventId,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(auctionService.getItems(user.getCommunity().getId(), eventId));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('View Events')")
    public ResponseEntity<EventAuctionItemResponse> getItem(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(auctionService.getItem(id, user.getCommunity().getId()));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('Create Event')")
    public ResponseEntity<EventAuctionItemResponse> createItem(
            @Valid @RequestBody EventAuctionItemRequest req,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(auctionService.createItem(req, user, user.getCommunity()));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('Create Event')")
    public ResponseEntity<EventAuctionItemResponse> updateItem(
            @PathVariable Long id,
            @Valid @RequestBody EventAuctionItemRequest req,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(auctionService.updateItem(id, req, user.getCommunity().getId()));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('Create Event')")
    public ResponseEntity<Void> deleteItem(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        auctionService.deleteItem(id, user.getCommunity().getId());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/bid")
    @PreAuthorize("hasAuthority('View Events')")
    public ResponseEntity<EventAuctionItemResponse> placeBid(
            @PathVariable Long id,
            @Valid @RequestBody EventAuctionBidRequest req,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(auctionService.placeBid(id, req, user, user.getCommunity()));
    }

    @GetMapping("/{id}/bids")
    @PreAuthorize("hasAuthority('View Events')")
    public ResponseEntity<List<EventAuctionBidResponse>> getBids(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(auctionService.getBids(id, user.getCommunity().getId()));
    }

    @GetMapping("/recent-bids")
    @PreAuthorize("hasAuthority('View Events')")
    public ResponseEntity<List<EventAuctionBidResponse>> getRecentBids(
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(auctionService.getRecentBids(user.getCommunity().getId()));
    }

    @GetMapping("/stats")
    @PreAuthorize("hasAuthority('View Events')")
    public ResponseEntity<EventAuctionStatsResponse> getStats(
            @RequestParam(required = false) Long eventId,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(auctionService.getStats(user.getCommunity().getId(), eventId));
    }
}
