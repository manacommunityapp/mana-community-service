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
@RequestMapping({"/api/events/sponsors", "/events/sponsors", "/api/events/{eventId}/sponsors", "/events/{eventId}/sponsors"})
@RequiredArgsConstructor
public class EventSponsorController {

    private final EventSponsorService sponsorService;
    private final LoggedInUserService loggedInUserService;

    @GetMapping
    @PreAuthorize("hasAuthority('View Events') or hasAnyRole('USER','RESIDENT','OWNER','TENANT','MEMBER','ADMIN','COMMUNITY_ADMIN','EVENT_ADMIN','EVENTS_ADMIN','SUPER_ADMIN')")
    public ResponseEntity<List<EventSponsorResponse>> getAll(
            @PathVariable(name = "eventId", required = false) Long pathEventId,
            @RequestParam(name = "eventId", required = false) Long queryEventId,
            @AuthenticationPrincipal UserPrincipal principal) {
        Long eventId = pathEventId != null ? pathEventId : queryEventId;
        if (eventId != null) {
            return ResponseEntity.ok(sponsorService.getByEvent(eventId));
        }
        AppUser user = loggedInUserService.resolve(principal);
        Long communityId = user != null && user.getCommunity() != null ? user.getCommunity().getId() : null;
        if (communityId == null) return ResponseEntity.ok(List.of());
        return ResponseEntity.ok(sponsorService.getByCommunity(communityId));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('Create Event') or hasAnyRole('ADMIN','COMMUNITY_ADMIN','EVENT_ADMIN','EVENTS_ADMIN','SUPER_ADMIN')")
    public ResponseEntity<EventSponsorResponse> create(
            @PathVariable(name = "eventId", required = false) Long pathEventId,
            @Valid @RequestBody EventSponsorRequest req,
            @AuthenticationPrincipal UserPrincipal principal) {
        if (req.getEventId() == null && pathEventId != null) {
            req.setEventId(pathEventId);
        }
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(sponsorService.create(req, user, user != null ? user.getCommunity() : null));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('Create Event') or hasAnyRole('ADMIN','COMMUNITY_ADMIN','EVENT_ADMIN','EVENTS_ADMIN','SUPER_ADMIN')")
    public ResponseEntity<EventSponsorResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody EventSponsorRequest req) {
        return ResponseEntity.ok(sponsorService.update(id, req));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('Create Event') or hasAnyRole('ADMIN','COMMUNITY_ADMIN','EVENT_ADMIN','EVENTS_ADMIN','SUPER_ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        sponsorService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
