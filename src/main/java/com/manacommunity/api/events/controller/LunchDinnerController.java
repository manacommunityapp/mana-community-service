package com.manacommunity.api.events.controller;

import com.manacommunity.api.events.entity.EventLunchDinner;
import com.manacommunity.api.events.service.LunchDinnerService;
import com.manacommunity.api.user.security.UserPrincipal;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/events/lunch-dinners")
public class LunchDinnerController {

    private final LunchDinnerService service;

    public LunchDinnerController(LunchDinnerService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<List<EventLunchDinner>> getAll(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false) Long mainEventId,
            @RequestParam(required = false) Long eventId) {
        Long targetEventId = mainEventId != null ? mainEventId : eventId;
        Long communityId = principal != null ? principal.getCommunityId() : null;
        List<EventLunchDinner> list = service.getAllLunchDinners(communityId, targetEventId);
        return ResponseEntity.ok(list);
    }

    @GetMapping("/{id}")
    public ResponseEntity<EventLunchDinner> getById(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id) {
        Long communityId = principal != null ? principal.getCommunityId() : null;
        EventLunchDinner item = service.getLunchDinnerById(id, communityId);
        return ResponseEntity.ok(item);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','COMMUNITY_ADMIN','EVENT_ADMIN','SUPER_ADMIN') or hasAuthority('Manage Event Forms')")
    public ResponseEntity<EventLunchDinner> create(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestBody EventLunchDinner body) {
        Long communityId = principal != null ? principal.getCommunityId() : null;
        EventLunchDinner created = service.createLunchDinner(communityId, body);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','COMMUNITY_ADMIN','EVENT_ADMIN','SUPER_ADMIN') or hasAuthority('Manage Event Forms')")
    public ResponseEntity<EventLunchDinner> update(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id,
            @RequestBody EventLunchDinner body) {
        Long communityId = principal != null ? principal.getCommunityId() : null;
        EventLunchDinner updated = service.updateLunchDinner(id, communityId, body);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','COMMUNITY_ADMIN','EVENT_ADMIN','SUPER_ADMIN') or hasAuthority('Manage Event Forms')")
    public ResponseEntity<Void> delete(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id) {
        Long communityId = principal != null ? principal.getCommunityId() : null;
        service.deleteLunchDinner(id, communityId);
        return ResponseEntity.noContent().build();
    }
}
