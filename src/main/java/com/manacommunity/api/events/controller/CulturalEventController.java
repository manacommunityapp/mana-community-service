package com.manacommunity.api.events.controller;

import com.manacommunity.api.events.entity.EventCulturalEvent;
import com.manacommunity.api.events.service.CulturalEventService;
import com.manacommunity.api.user.security.UserPrincipal;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/events/cultural-events")
public class CulturalEventController {

    private final CulturalEventService service;

    public CulturalEventController(CulturalEventService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<List<EventCulturalEvent>> getAll(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false) Long mainEventId) {
        Long communityId = principal != null ? principal.getCommunityId() : null;
        List<EventCulturalEvent> list = service.getAllCulturalEvents(communityId, mainEventId);
        return ResponseEntity.ok(list);
    }

    @GetMapping("/{id}")
    public ResponseEntity<EventCulturalEvent> getById(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id) {
        Long communityId = principal != null ? principal.getCommunityId() : null;
        EventCulturalEvent item = service.getCulturalEventById(id, communityId);
        return ResponseEntity.ok(item);
    }

    @PostMapping
    @org.springframework.security.access.prepost.PreAuthorize("hasAnyRole('ADMIN','COMMUNITY_ADMIN','EVENT_ADMIN','SUPER_ADMIN') or hasAuthority('Manage Event Forms')")
    public ResponseEntity<EventCulturalEvent> create(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestBody EventCulturalEvent body) {
        Long communityId = principal != null ? principal.getCommunityId() : null;
        EventCulturalEvent created = service.createCulturalEvent(communityId, body);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    @org.springframework.security.access.prepost.PreAuthorize("hasAnyRole('ADMIN','COMMUNITY_ADMIN','EVENT_ADMIN','SUPER_ADMIN') or hasAuthority('Manage Event Forms')")
    public ResponseEntity<EventCulturalEvent> update(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id,
            @RequestBody EventCulturalEvent body) {
        Long communityId = principal != null ? principal.getCommunityId() : null;
        EventCulturalEvent updated = service.updateCulturalEvent(id, communityId, body);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    @org.springframework.security.access.prepost.PreAuthorize("hasAnyRole('ADMIN','COMMUNITY_ADMIN','EVENT_ADMIN','SUPER_ADMIN') or hasAuthority('Manage Event Forms')")
    public ResponseEntity<Void> delete(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id) {
        Long communityId = principal != null ? principal.getCommunityId() : null;
        service.deleteCulturalEvent(id, communityId);
        return ResponseEntity.noContent().build();
    }
}
