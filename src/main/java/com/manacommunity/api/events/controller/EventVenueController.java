package com.manacommunity.api.events.controller;

import com.manacommunity.api.events.entity.EventVenue;
import com.manacommunity.api.events.service.EventVenueService;
import com.manacommunity.api.user.security.UserPrincipal;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/events/venues")
public class EventVenueController {

    private final EventVenueService service;

    public EventVenueController(EventVenueService service) {
        this.service = service;
    }

    @GetMapping
    @PreAuthorize("hasAnyAuthority('View Events', 'Register Event', 'View Event Dashboard') or hasAnyRole('USER', 'MEMBER', 'ADMIN', 'COMMUNITY_ADMIN', 'EVENT_ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<List<EventVenue>> getVenues(
            @RequestParam(required = false) String status,
            @AuthenticationPrincipal UserPrincipal principal) {
        Long communityId = (principal != null) ? principal.getCommunityId() : null;
        return ResponseEntity.ok(service.getVenues(communityId, status));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('View Events', 'Register Event', 'View Event Dashboard') or hasAnyRole('USER', 'MEMBER', 'ADMIN', 'COMMUNITY_ADMIN', 'EVENT_ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<EventVenue> getVenueById(@PathVariable Long id) {
        return ResponseEntity.ok(service.getVenueById(id));
    }

    @PostMapping
    @PreAuthorize("hasAnyAuthority('Create Event', 'Create/Edit Event Schedule', 'Manage Event Dashboard') or hasAnyRole('ADMIN', 'COMMUNITY_ADMIN', 'EVENT_ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<EventVenue> createVenue(
            @RequestBody EventVenue venue,
            @AuthenticationPrincipal UserPrincipal principal) {
        Long communityId = (principal != null) ? principal.getCommunityId() : null;
        return ResponseEntity.status(HttpStatus.CREATED).body(service.createVenue(venue, communityId));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('Create Event', 'Create/Edit Event Schedule', 'Manage Event Dashboard') or hasAnyRole('ADMIN', 'COMMUNITY_ADMIN', 'EVENT_ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<EventVenue> updateVenue(
            @PathVariable Long id,
            @RequestBody EventVenue venue) {
        return ResponseEntity.ok(service.updateVenue(id, venue));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('Create Event', 'Delete Event Schedule', 'Manage Event Dashboard') or hasAnyRole('ADMIN', 'COMMUNITY_ADMIN', 'EVENT_ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<Void> deleteVenue(@PathVariable Long id) {
        service.deleteVenue(id);
        return ResponseEntity.noContent().build();
    }
}