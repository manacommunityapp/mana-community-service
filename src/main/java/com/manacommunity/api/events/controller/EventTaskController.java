package com.manacommunity.api.events.controller;

import com.manacommunity.api.events.dto.EventTaskRequest;
import com.manacommunity.api.events.dto.EventTaskResponse;
import com.manacommunity.api.events.service.EventTaskService;
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
@RequestMapping({"/api/events/tasks", "/events/tasks", "/api/events/{eventId}/tasks", "/events/{eventId}/tasks"})
@RequiredArgsConstructor
public class EventTaskController {

    private final EventTaskService taskService;
    private final LoggedInUserService loggedInUserService;

    @GetMapping
    @PreAuthorize("hasAuthority('View Events') or hasAnyRole('USER','RESIDENT','OWNER','TENANT','MEMBER','ADMIN','COMMUNITY_ADMIN','EVENT_ADMIN','EVENTS_ADMIN','SUPER_ADMIN')")
    public ResponseEntity<List<EventTaskResponse>> getAll(
            @PathVariable(name = "eventId", required = false) Long pathEventId,
            @RequestParam(name = "eventId", required = false) Long queryEventId,
            @AuthenticationPrincipal UserPrincipal principal) {
        Long eventId = pathEventId != null ? pathEventId : queryEventId;
        AppUser user = loggedInUserService.resolve(principal);
        if (eventId != null) {
            return ResponseEntity.ok(taskService.getByEvent(eventId));
        }
        Long communityId = (user != null && user.getCommunity() != null) ? user.getCommunity().getId() : null;
        if (communityId == null) return ResponseEntity.ok(List.of());
        return ResponseEntity.ok(taskService.getByCommunity(communityId));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('Create Event') or hasAnyRole('ADMIN','COMMUNITY_ADMIN','EVENT_ADMIN','EVENTS_ADMIN','SUPER_ADMIN')")
    public ResponseEntity<EventTaskResponse> create(
            @PathVariable(name = "eventId", required = false) Long pathEventId,
            @Valid @RequestBody EventTaskRequest req,
            @AuthenticationPrincipal UserPrincipal principal) {
        if (req.getEventId() == null && pathEventId != null) {
            req.setEventId(pathEventId);
        }
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(taskService.create(req, user, user != null ? user.getCommunity() : null));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('Create Event') or hasAnyRole('ADMIN','COMMUNITY_ADMIN','EVENT_ADMIN','EVENTS_ADMIN','SUPER_ADMIN')")
    public ResponseEntity<EventTaskResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody EventTaskRequest req) {
        return ResponseEntity.ok(taskService.update(id, req));
    }

    @RequestMapping(value = "/{id}/toggle", method = {RequestMethod.PUT, RequestMethod.PATCH})
    @PreAuthorize("hasAuthority('Create Event') or hasAnyRole('ADMIN','COMMUNITY_ADMIN','EVENT_ADMIN','EVENTS_ADMIN','SUPER_ADMIN')")
    public ResponseEntity<EventTaskResponse> toggleDone(@PathVariable Long id) {
        return ResponseEntity.ok(taskService.toggleDone(id));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('Create Event') or hasAnyRole('ADMIN','COMMUNITY_ADMIN','EVENT_ADMIN','EVENTS_ADMIN','SUPER_ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        taskService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
