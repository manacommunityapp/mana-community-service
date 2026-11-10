package com.manacommunity.api.market.bazaar.controller;

import com.manacommunity.api.market.bazaar.dto.MarketBazaarDtos.*;
import com.manacommunity.api.market.bazaar.service.MarketBazaarService;
import com.manacommunity.api.payments.dto.CashfreeOrderResponse;
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
import java.util.Map;

@RestController
@RequestMapping("/api/market")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class MarketBazaarController {
    private final MarketBazaarService marketService;
    private final LoggedInUserService loggedInUserService;

    @GetMapping("/events")
    public List<EventResponse> listEvents(@AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false) String query, @RequestParam(required = false) String city,
            @RequestParam(required = false) String category, @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "30") int size) {
        return marketService.listEvents(user(principal), query, city, category, page, size);
    }
    @PreAuthorize("hasAnyRole('ADMIN','COMMUNITY_ADMIN','EVENT_ADMIN','SUPER_ADMIN') or hasAnyAuthority('Create Event','Manage Event Dashboard')")
    @GetMapping("/organizer/events")
    public List<EventResponse> organizerEvents(@AuthenticationPrincipal UserPrincipal principal) {
        return marketService.listOrganizerEvents(user(principal));
    }
    @PreAuthorize("hasAnyRole('ADMIN','COMMUNITY_ADMIN','EVENT_ADMIN','SUPER_ADMIN') or hasAnyAuthority('Create Event','Manage Event Dashboard')")
    @PostMapping("/events")
    public ResponseEntity<EventResponse> createEvent(@AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody CreateEventRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(marketService.createEvent(user(principal), request));
    }
    @GetMapping("/events/{eventId}/stalls")
    public List<StallResponse> stalls(@AuthenticationPrincipal UserPrincipal principal, @PathVariable Long eventId) {
        return marketService.listStalls(user(principal), eventId);
    }
    @PreAuthorize("hasAnyRole('ADMIN','COMMUNITY_ADMIN','EVENT_ADMIN','SUPER_ADMIN') or hasAnyAuthority('Create Event','Manage Event Dashboard')")
    @PostMapping("/events/{eventId}/stalls")
    public ResponseEntity<StallResponse> createStall(@AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long eventId, @Valid @RequestBody CreateStallRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(marketService.createStall(user(principal), eventId, request));
    }
    @PostMapping("/events/{eventId}/applications")
    public ResponseEntity<ApplicationResponse> apply(@AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long eventId, @Valid @RequestBody ApplyRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(marketService.apply(user(principal), eventId, request));
    }
    @PreAuthorize("hasAnyRole('ADMIN','COMMUNITY_ADMIN','EVENT_ADMIN','SUPER_ADMIN') or hasAnyAuthority('Create Event','Manage Event Dashboard')")
    @GetMapping("/events/{eventId}/applications")
    public List<ApplicationResponse> applications(@AuthenticationPrincipal UserPrincipal principal, @PathVariable Long eventId) {
        return marketService.listApplications(user(principal), eventId);
    }
    @GetMapping("/vendors/me/bookings")
    public List<ApplicationResponse> myApplications(@AuthenticationPrincipal UserPrincipal principal) {
        return marketService.listMyApplications(user(principal));
    }
    @PreAuthorize("hasAnyRole('ADMIN','COMMUNITY_ADMIN','EVENT_ADMIN','SUPER_ADMIN') or hasAnyAuthority('Create Event','Manage Event Dashboard')")
    @PostMapping("/applications/{applicationId}/approve")
    public ApplicationResponse approve(@AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long applicationId, @Valid @RequestBody ApproveRequest request) {
        return marketService.approve(user(principal), applicationId, request.stallId());
    }
    @PreAuthorize("hasAnyRole('ADMIN','COMMUNITY_ADMIN','EVENT_ADMIN','SUPER_ADMIN') or hasAnyAuthority('Create Event','Manage Event Dashboard')")
    @PostMapping("/applications/{applicationId}/reject")
    public ApplicationResponse reject(@AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long applicationId, @Valid @RequestBody RejectRequest request) {
        return marketService.reject(user(principal), applicationId, request.reason());
    }
    @PostMapping("/bookings/{bookingId}/checkout")
    public CashfreeOrderResponse checkout(@AuthenticationPrincipal UserPrincipal principal, @PathVariable Long bookingId) {
        return marketService.checkout(user(principal), bookingId);
    }
    @PreAuthorize("hasAnyRole('ADMIN','COMMUNITY_ADMIN','EVENT_ADMIN','SUPER_ADMIN') or hasAnyAuthority('Create Event','Manage Event Dashboard')")
    @GetMapping("/events/{eventId}/analytics")
    public EventAnalyticsResponse analytics(@AuthenticationPrincipal UserPrincipal principal, @PathVariable Long eventId) {
        return marketService.analytics(user(principal), eventId);
    }
    private AppUser user(UserPrincipal principal) { return loggedInUserService.resolve(principal); }
    public record ApproveRequest(@jakarta.validation.constraints.NotNull Long stallId) {}
}


