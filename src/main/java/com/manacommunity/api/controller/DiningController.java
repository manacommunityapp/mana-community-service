package com.manacommunity.api.controller;

import com.manacommunity.api.dto.DiningEventRequest;
import com.manacommunity.api.dto.DiningEventResponse;
import com.manacommunity.api.dto.DiningRsvpRequest;
import com.manacommunity.api.dto.DiningRsvpResponse;
import com.manacommunity.api.service.DiningService;
import com.manacommunity.api.user.model.AppUser;
import com.manacommunity.api.user.security.UserPrincipal;
import com.manacommunity.api.user.service.LoggedInUserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/food/dining")
@RequiredArgsConstructor
public class DiningController {

    private final DiningService diningService;
    private final LoggedInUserService loggedInUserService;

    @GetMapping("/events")
    public ResponseEntity<List<DiningEventResponse>> getEvents(
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(diningService.getCommunityEvents(communityId(principal)));
    }

    @GetMapping("/events/{id}")
    public ResponseEntity<DiningEventResponse> getEvent(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id) {
        return ResponseEntity.ok(diningService.getEvent(id, communityId(principal)));
    }

    @PostMapping("/events")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    public ResponseEntity<DiningEventResponse> createEvent(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody DiningEventRequest request) {
        AppUser user = requireMember(principal);
        return ResponseEntity.ok(diningService.createEvent(request, user));
    }

    @PostMapping("/rsvp")
    public ResponseEntity<DiningRsvpResponse> createRsvp(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody DiningRsvpRequest request) {
        AppUser user = requireMember(principal);
        return ResponseEntity.ok(diningService.createRsvp(request, user));
    }

    @DeleteMapping("/rsvp/{id}")
    public ResponseEntity<Void> cancelRsvp(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id) {
        AppUser user = requireMember(principal);
        diningService.cancelRsvp(id, user.getId());
        return ResponseEntity.noContent().build();
    }

    private Long communityId(UserPrincipal principal) {
        return requireMember(principal).getCommunity().getId();
    }

    private AppUser requireMember(UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        if (user.getCommunity() == null) {
            throw new IllegalArgumentException("Dining features are scoped to a community.");
        }
        return user;
    }
}
