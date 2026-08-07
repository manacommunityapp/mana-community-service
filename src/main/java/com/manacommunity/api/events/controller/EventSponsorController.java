package com.manacommunity.api.events.controller;

import com.manacommunity.api.events.dto.EventSponsorRequest;
import com.manacommunity.api.events.dto.EventSponsorResponse;
import com.manacommunity.api.events.service.EventSponsorService;
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
@RequestMapping("/api/events/sponsors")
@RequiredArgsConstructor
public class EventSponsorController {

    private final EventSponsorService sponsorService;
    private final LoggedInUserService loggedInUserService;

    @GetMapping
    @PreAuthorize("hasAuthority('View Events')")
    public ResponseEntity<List<EventSponsorResponse>> getAll(
            @RequestParam(required = false) Long eventId,
            @AuthenticationPrincipal UserPrincipal principal) {
        if (eventId != null) {
            return ResponseEntity.ok(sponsorService.getByEvent(eventId));
        }
        AppUser user = loggedInUserService.resolve(principal);
        Long communityId = user.getCommunity() != null ? user.getCommunity().getId() : null;
        if (communityId == null) return ResponseEntity.ok(List.of());
        return ResponseEntity.ok(sponsorService.getByCommunity(communityId));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('Create Event')")
    public ResponseEntity<EventSponsorResponse> create(
            @Valid @RequestBody EventSponsorRequest req,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(sponsorService.create(req, user, user.getCommunity()));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('Create Event')")
    public ResponseEntity<EventSponsorResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody EventSponsorRequest req) {
        return ResponseEntity.ok(sponsorService.update(id, req));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('Create Event')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        sponsorService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
