package com.manacommunity.api.events.controller;

import com.manacommunity.api.events.dto.EventRequest;
import com.manacommunity.api.events.dto.EventResponse;
import com.manacommunity.api.events.service.EventService;
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
@RequestMapping("/api/events")
@RequiredArgsConstructor
public class EventController {

    private final EventService eventService;
    private final LoggedInUserService loggedInUserService;

    @GetMapping
    @PreAuthorize("hasAuthority('View Events')")
    public ResponseEntity<List<EventResponse>> getUpcomingEvents(
            @RequestParam(required = false) String type,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        Long communityId = user.getCommunity() != null ? user.getCommunity().getId() : null;
        if (communityId == null) return ResponseEntity.ok(List.of());
        return ResponseEntity.ok(eventService.getUpcomingEvents(communityId, type, user.getId()));
    }

    @GetMapping("/all")
    @PreAuthorize("hasAuthority('View Events')")
    public ResponseEntity<List<EventResponse>> getAllEvents(
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        Long communityId = user.getCommunity() != null ? user.getCommunity().getId() : null;
        if (communityId == null) return ResponseEntity.ok(List.of());
        return ResponseEntity.ok(eventService.getAllEvents(communityId, user.getId()));
    }

    @GetMapping("/mine")
    @PreAuthorize("hasAuthority('View Events')")
    public ResponseEntity<List<EventResponse>> getMyEvents(
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(eventService.getMyEvents(user.getId()));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('View Events')")
    public ResponseEntity<EventResponse> getById(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(eventService.getById(id, user.getId()));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('Create Event')")
    public ResponseEntity<EventResponse> create(
            @Valid @RequestBody EventRequest req,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(eventService.create(req, user, user.getCommunity()));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('Create Event')")
    public ResponseEntity<EventResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody EventRequest req,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(eventService.update(id, req, user.getId()));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('Create Event')")
    public ResponseEntity<Void> delete(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        eventService.delete(id, user.getId());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/register")
    @PreAuthorize("hasAuthority('Register Event')")
    public ResponseEntity<EventResponse> register(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(eventService.register(id, user));
    }

    @DeleteMapping("/{id}/register")
    @PreAuthorize("hasAuthority('Register Event')")
    public ResponseEntity<EventResponse> unregister(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(eventService.unregister(id, user.getId()));
    }
}
