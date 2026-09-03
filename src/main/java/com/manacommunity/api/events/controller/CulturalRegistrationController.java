package com.manacommunity.api.events.controller;

import com.manacommunity.api.events.entity.EventCulturalRegistration;
import com.manacommunity.api.events.service.CulturalRegistrationService;
import com.manacommunity.api.user.model.AppUser;
import com.manacommunity.api.user.security.UserPrincipal;
import com.manacommunity.api.user.service.LoggedInUserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/events/cultural/registrations")
public class CulturalRegistrationController {

    private final CulturalRegistrationService service;
    private final LoggedInUserService loggedInUserService;

    public CulturalRegistrationController(CulturalRegistrationService service,
                                          LoggedInUserService loggedInUserService) {
        this.service = service;
        this.loggedInUserService = loggedInUserService;
    }

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<EventCulturalRegistration> createRegistration(
            @RequestBody EventCulturalRegistration request,
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestHeader(value = "X-Community-Id", required = false) Long communityId,
            @RequestParam(value = "adminOverride", required = false, defaultValue = "false") boolean adminOverride) {
        AppUser caller = loggedInUserService.resolve(principal);
        EventCulturalRegistration created = service.createRegistration(request, caller, communityId, adminOverride);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping("/my")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<EventCulturalRegistration>> getMyRegistrations(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestHeader(value = "X-Community-Id", required = false) Long communityId) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(service.getMyRegistrations(user, communityId));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN','COMMUNITY_ADMIN','EVENT_ADMIN')")
    public ResponseEntity<List<EventCulturalRegistration>> getRegistrationsByCommunity(
            @RequestHeader(value = "X-Community-Id", required = false) Long communityId) {
        return ResponseEntity.ok(service.getRegistrationsByCommunity(communityId));
    }

    @GetMapping("/event/{culturalEventId}")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN','COMMUNITY_ADMIN','EVENT_ADMIN')")
    public ResponseEntity<List<EventCulturalRegistration>> getRegistrationsByCulturalEvent(
            @PathVariable Long culturalEventId) {
        return ResponseEntity.ok(service.getRegistrationsByCulturalEvent(culturalEventId));
    }

    @GetMapping("/main-event/{mainEventId}")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN','COMMUNITY_ADMIN','EVENT_ADMIN')")
    public ResponseEntity<List<EventCulturalRegistration>> getRegistrationsByMainEvent(
            @PathVariable Long mainEventId) {
        return ResponseEntity.ok(service.getRegistrationsByMainEvent(mainEventId));
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<EventCulturalRegistration> getById(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser caller = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(service.getById(id, caller));
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> cancelRegistration(
            @PathVariable Long id,
            @RequestBody(required = false) Map<String, String> body,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser caller = loggedInUserService.resolve(principal);
        String reason = body != null ? body.get("reason") : null;
        service.cancelRegistration(id, reason, caller);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/checkin")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN','COMMUNITY_ADMIN','EVENT_ADMIN')")
    public ResponseEntity<EventCulturalRegistration> checkIn(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser caller = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(service.checkIn(id, caller));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN','COMMUNITY_ADMIN','EVENT_ADMIN')")
    public ResponseEntity<Void> deleteRegistration(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser caller = loggedInUserService.resolve(principal);
        service.deleteRegistration(id, caller);
        return ResponseEntity.noContent().build();
    }
}
