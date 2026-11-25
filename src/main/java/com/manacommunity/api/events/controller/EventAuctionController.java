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
import com.manacommunity.platform.websocket.EventAuctionRedisPublisher;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/events/auction-items")
@RequiredArgsConstructor
public class EventAuctionController {

    private final EventAuctionService auctionService;
    private final LoggedInUserService loggedInUserService;

    @Autowired(required = false)
    private EventAuctionRedisPublisher eventAuctionRedisPublisher;

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
        EventAuctionItemResponse updated = auctionService.updateItem(id, req, user.getCommunity().getId());
        publishAuctionEvent("ITEM_UPDATED", updated, null);
        return ResponseEntity.ok(updated);
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
        EventAuctionItemResponse updated = auctionService.placeBid(id, req, user, user.getCommunity());
        publishAuctionEvent("BID_PLACED", updated, user.getId());
        return ResponseEntity.ok(updated);
    }

    private void publishAuctionEvent(String type, EventAuctionItemResponse updated, Long bidderId) {
        if (eventAuctionRedisPublisher == null || updated == null || updated.getId() == null) {
            return;
        }
        Map<String, Object> event = new HashMap<>();
        event.put("type", type);
        event.put("auctionId", updated.getId());
        event.put("amount", updated.getCurrentBid());
        if (bidderId != null) {
            event.put("bidderId", bidderId);
        }
        event.put("bidderName", updated.getLeaderName() != null ? updated.getLeaderName() : "");
        event.put("bidCount", updated.getBidCount());
        event.put("status", updated.getStatus() != null ? updated.getStatus() : "");
        event.put("timestamp", Instant.now().toString());
        eventAuctionRedisPublisher.publishBidEvent(updated.getId(), event);
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
