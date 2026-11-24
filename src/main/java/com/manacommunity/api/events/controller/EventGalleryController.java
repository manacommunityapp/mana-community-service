package com.manacommunity.api.events.controller;

import com.manacommunity.api.events.dto.EventGalleryItemRequest;
import com.manacommunity.api.events.dto.EventGalleryItemResponse;
import com.manacommunity.api.events.service.EventGalleryService;
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
@RequestMapping({"/api/events/gallery", "/events/gallery", "/api/events/{eventId}/gallery", "/events/{eventId}/gallery"})
@RequiredArgsConstructor
public class EventGalleryController {

    private final EventGalleryService galleryService;
    private final LoggedInUserService loggedInUserService;

    @GetMapping
    @PreAuthorize("hasAuthority('View Events') or hasAnyRole('USER','RESIDENT','OWNER','TENANT','MEMBER','ADMIN','COMMUNITY_ADMIN','EVENT_ADMIN','EVENTS_ADMIN','SUPER_ADMIN')")
    public ResponseEntity<List<EventGalleryItemResponse>> getByEvent(
            @PathVariable(name = "eventId", required = false) Long pathEventId,
            @RequestParam(name = "eventId", required = false) Long queryEventId,
            @RequestParam(required = false) String albumName,
            @RequestParam(required = false) String album) {
        Long eventId = pathEventId != null ? pathEventId : queryEventId;
        String effectiveAlbum = albumName != null ? albumName : album;
        return ResponseEntity.ok(galleryService.getByEvent(eventId, effectiveAlbum));
    }

    @GetMapping("/community")
    @PreAuthorize("hasAuthority('View Events') or hasAnyRole('USER','RESIDENT','OWNER','TENANT','MEMBER','ADMIN','COMMUNITY_ADMIN','EVENT_ADMIN','EVENTS_ADMIN','SUPER_ADMIN')")
    public ResponseEntity<List<EventGalleryItemResponse>> getByCommunity(
            @RequestParam(required = false) String dayTag,
            @RequestParam(required = false) String category,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        Long communityId = user != null && user.getCommunity() != null ? user.getCommunity().getId() : null;
        return ResponseEntity.ok(galleryService.getByCommunity(communityId, dayTag, category));
    }

    @GetMapping("/albums")
    @PreAuthorize("hasAuthority('View Events') or hasAnyRole('USER','RESIDENT','OWNER','TENANT','MEMBER','ADMIN','COMMUNITY_ADMIN','EVENT_ADMIN','EVENTS_ADMIN','SUPER_ADMIN')")
    public ResponseEntity<List<String>> getAlbums(
            @PathVariable(name = "eventId", required = false) Long pathEventId,
            @RequestParam(name = "eventId", required = false) Long queryEventId) {
        Long eventId = pathEventId != null ? pathEventId : queryEventId;
        return ResponseEntity.ok(galleryService.getAlbums(eventId));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('Manage Event Media') or hasAuthority('Create Event') or hasAnyRole('USER','RESIDENT','OWNER','TENANT','MEMBER','ADMIN','COMMUNITY_ADMIN','EVENT_ADMIN','EVENTS_ADMIN','SUPER_ADMIN')")
    public ResponseEntity<EventGalleryItemResponse> create(
            @PathVariable(name = "eventId", required = false) Long pathEventId,
            @Valid @RequestBody EventGalleryItemRequest req,
            @AuthenticationPrincipal UserPrincipal principal) {
        if (req.getEventId() == null && pathEventId != null) {
            req.setEventId(pathEventId);
        }
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(galleryService.create(req, user, user != null ? user.getCommunity() : null));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('Manage Event Media') or hasAuthority('Create Event') or hasAnyRole('ADMIN','COMMUNITY_ADMIN','EVENT_ADMIN','EVENTS_ADMIN','SUPER_ADMIN')")
    public ResponseEntity<EventGalleryItemResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody EventGalleryItemRequest req) {
        return ResponseEntity.ok(galleryService.update(id, req));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('Manage Event Media') or hasAuthority('Create Event') or hasAnyRole('ADMIN','COMMUNITY_ADMIN','EVENT_ADMIN','EVENTS_ADMIN','SUPER_ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        galleryService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
