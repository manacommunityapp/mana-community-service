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
@RequestMapping("/api/events/gallery")
@RequiredArgsConstructor
public class EventGalleryController {

    private final EventGalleryService galleryService;
    private final LoggedInUserService loggedInUserService;

    @GetMapping
    @PreAuthorize("hasAuthority('View Events')")
    public ResponseEntity<List<EventGalleryItemResponse>> getByEvent(
            @RequestParam Long eventId,
            @RequestParam(required = false) String albumName) {
        return ResponseEntity.ok(galleryService.getByEvent(eventId, albumName));
    }

    @GetMapping("/community")
    @PreAuthorize("hasAuthority('View Events')")
    public ResponseEntity<List<EventGalleryItemResponse>> getByCommunity(
            @RequestParam(required = false) String dayTag,
            @RequestParam(required = false) String category,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(galleryService.getByCommunity(user.getCommunity().getId(), dayTag, category));
    }

    @GetMapping("/albums")
    @PreAuthorize("hasAuthority('View Events')")
    public ResponseEntity<List<String>> getAlbums(@RequestParam Long eventId) {
        return ResponseEntity.ok(galleryService.getAlbums(eventId));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('Create Event')")
    public ResponseEntity<EventGalleryItemResponse> create(
            @Valid @RequestBody EventGalleryItemRequest req,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(galleryService.create(req, user, user.getCommunity()));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('Create Event')")
    public ResponseEntity<EventGalleryItemResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody EventGalleryItemRequest req) {
        return ResponseEntity.ok(galleryService.update(id, req));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('Create Event')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        galleryService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
