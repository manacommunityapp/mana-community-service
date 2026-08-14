package com.manacommunity.api.events.controller;

import com.manacommunity.api.events.dto.EventVolunteerRequest;
import com.manacommunity.api.events.dto.EventVolunteerResponse;
import com.manacommunity.api.events.service.EventVolunteerService;
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
@RequestMapping("/api/events/volunteers")
@RequiredArgsConstructor
public class EventVolunteerController {

    private final EventVolunteerService volunteerService;
    private final LoggedInUserService loggedInUserService;

    @GetMapping
    @PreAuthorize("hasAuthority('View Events')")
    public ResponseEntity<List<EventVolunteerResponse>> getAll(
            @RequestParam(required = false) Long eventId,
            @AuthenticationPrincipal UserPrincipal principal) {
        if (eventId != null) {
            return ResponseEntity.ok(volunteerService.getByEvent(eventId));
        }
        AppUser user = loggedInUserService.resolve(principal);
        Long communityId = user != null && user.getCommunity() != null ? user.getCommunity().getId() : null;
        return ResponseEntity.ok(volunteerService.getByCommunity(communityId));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('Create Event')")
    public ResponseEntity<EventVolunteerResponse> create(
            @Valid @RequestBody EventVolunteerRequest req,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(volunteerService.create(req, user.getCommunity()));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('Create Event')")
    public ResponseEntity<EventVolunteerResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody EventVolunteerRequest req) {
        return ResponseEntity.ok(volunteerService.update(id, req));
    }

    @PutMapping("/{id}/check-in")
    @PreAuthorize("hasAuthority('Create Event')")
    public ResponseEntity<EventVolunteerResponse> checkIn(@PathVariable Long id) {
        return ResponseEntity.ok(volunteerService.checkIn(id));
    }

    @PutMapping("/{id}/check-out")
    @PreAuthorize("hasAuthority('Create Event')")
    public ResponseEntity<EventVolunteerResponse> checkOut(@PathVariable Long id) {
        return ResponseEntity.ok(volunteerService.checkOut(id));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('Create Event')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        volunteerService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
