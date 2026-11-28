package com.manacommunity.api.events.controller;

import com.manacommunity.api.events.dto.EventProgramRequest;
import com.manacommunity.api.events.dto.EventProgramResponse;
import com.manacommunity.api.events.service.EventProgramService;
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
@RequestMapping({"/api/events/programs", "/events/programs", "/api/events/{eventId}/programs", "/events/{eventId}/programs"})
@RequiredArgsConstructor
public class EventProgramController {

    private final EventProgramService programService;
    private final LoggedInUserService loggedInUserService;

    @GetMapping
    @PreAuthorize("hasAuthority('View Events') or hasAnyRole('USER','RESIDENT','OWNER','TENANT','MEMBER','ADMIN','COMMUNITY_ADMIN','EVENT_ADMIN','EVENTS_ADMIN','SUPER_ADMIN')")
    public ResponseEntity<List<EventProgramResponse>> getByEvent(
            @PathVariable(name = "eventId", required = false) Long pathEventId,
            @RequestParam(name = "eventId", required = false) Long queryEventId,
            @RequestParam(required = false) String dayLabel) {
        Long eventId = pathEventId != null ? pathEventId : queryEventId;
        return ResponseEntity.ok(programService.getByEvent(eventId, dayLabel));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('Create Event') or hasAnyRole('ADMIN','COMMUNITY_ADMIN','EVENT_ADMIN','EVENTS_ADMIN','SUPER_ADMIN')")
    public ResponseEntity<EventProgramResponse> create(
            @PathVariable(name = "eventId", required = false) Long pathEventId,
            @Valid @RequestBody EventProgramRequest req,
            @AuthenticationPrincipal UserPrincipal principal) {
        if (req.getEventId() == null && pathEventId != null) {
            req.setEventId(pathEventId);
        }
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(programService.create(req, user, user != null ? user.getCommunity() : null));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('Create Event') or hasAnyRole('ADMIN','COMMUNITY_ADMIN','EVENT_ADMIN','EVENTS_ADMIN','SUPER_ADMIN')")
    public ResponseEntity<EventProgramResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody EventProgramRequest req) {
        return ResponseEntity.ok(programService.update(id, req));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('Create Event') or hasAnyRole('ADMIN','COMMUNITY_ADMIN','EVENT_ADMIN','EVENTS_ADMIN','SUPER_ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        programService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
