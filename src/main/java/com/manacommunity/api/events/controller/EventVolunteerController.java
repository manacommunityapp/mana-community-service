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
@RequestMapping({"/api/events/volunteers", "/events/volunteers", "/api/events/{eventId}/volunteers", "/events/{eventId}/volunteers"})
@RequiredArgsConstructor
public class EventVolunteerController {

    private final EventVolunteerService volunteerService;
    private final LoggedInUserService loggedInUserService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','COMMUNITY_ADMIN','SUPER_ADMIN','EVENT_ADMIN','EVENTS_ADMIN','USER','RESIDENT','OWNER','TENANT','MEMBER') or hasAuthority('View Events')")
    public ResponseEntity<List<EventVolunteerResponse>> getAll(
            @PathVariable(name = "eventId", required = false) Long pathEventId,
            @RequestParam(name = "eventId", required = false) Long queryEventId,
            @AuthenticationPrincipal UserPrincipal principal) {
        Long eventId = pathEventId != null ? pathEventId : queryEventId;
        if (eventId != null) {
            return ResponseEntity.ok(volunteerService.getByEvent(eventId));
        }
        AppUser user = loggedInUserService.resolve(principal);
        Long communityId = user != null && user.getCommunity() != null ? user.getCommunity().getId() : null;
        return ResponseEntity.ok(volunteerService.getByCommunity(communityId));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','COMMUNITY_ADMIN','SUPER_ADMIN','EVENT_ADMIN','EVENTS_ADMIN','USER','RESIDENT','OWNER','TENANT','MEMBER') or hasAuthority('Create Event')")
    public ResponseEntity<EventVolunteerResponse> create(
            @PathVariable(name = "eventId", required = false) Long pathEventId,
            @Valid @RequestBody EventVolunteerRequest req,
            @AuthenticationPrincipal UserPrincipal principal) {
        if (req.getEventId() == null && pathEventId != null) {
            req.setEventId(pathEventId);
        }
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(volunteerService.create(req, user != null ? user.getCommunity() : null));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','COMMUNITY_ADMIN','SUPER_ADMIN','EVENT_ADMIN','EVENTS_ADMIN') or hasAuthority('Create Event')")
    public ResponseEntity<EventVolunteerResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody EventVolunteerRequest req) {
        return ResponseEntity.ok(volunteerService.update(id, req));
    }

    @RequestMapping(value = "/{id}/check-in", method = {RequestMethod.PUT, RequestMethod.POST})
    @PreAuthorize("hasAnyRole('ADMIN','COMMUNITY_ADMIN','SUPER_ADMIN','EVENT_ADMIN','EVENTS_ADMIN','USER','RESIDENT','OWNER','TENANT','MEMBER') or hasAuthority('Create Event')")
    public ResponseEntity<EventVolunteerResponse> checkIn(@PathVariable Long id) {
        return ResponseEntity.ok(volunteerService.checkIn(id));
    }

    @RequestMapping(value = "/{id}/check-out", method = {RequestMethod.PUT, RequestMethod.POST})
    @PreAuthorize("hasAnyRole('ADMIN','COMMUNITY_ADMIN','SUPER_ADMIN','EVENT_ADMIN','EVENTS_ADMIN','USER','RESIDENT','OWNER','TENANT','MEMBER') or hasAuthority('Create Event')")
    public ResponseEntity<EventVolunteerResponse> checkOut(@PathVariable Long id) {
        return ResponseEntity.ok(volunteerService.checkOut(id));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','COMMUNITY_ADMIN','SUPER_ADMIN','EVENT_ADMIN','EVENTS_ADMIN') or hasAuthority('Create Event')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        volunteerService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
