package com.manacommunity.api.events.controller;

import com.manacommunity.api.events.entity.EventPoojaUserRegistration;
import com.manacommunity.api.events.service.EventPoojaUserRegistrationService;
import com.manacommunity.api.user.model.AppUser;
import com.manacommunity.api.user.security.UserPrincipal;
import com.manacommunity.api.user.service.LoggedInUserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/events/pooja-registrations")
public class EventPoojaUserRegistrationController {

    private final EventPoojaUserRegistrationService service;
    private final LoggedInUserService loggedInUserService;

    public EventPoojaUserRegistrationController(
            EventPoojaUserRegistrationService service,
            LoggedInUserService loggedInUserService) {
        this.service = service;
        this.loggedInUserService = loggedInUserService;
    }

    @PostMapping
    public ResponseEntity<EventPoojaUserRegistration> createRegistration(
            @RequestBody EventPoojaUserRegistration registration,
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestHeader(value = "X-Community-Id", required = false) Long communityId,
            @RequestParam(value = "adminOverride", required = false, defaultValue = "false") boolean adminOverride) {
        AppUser user = loggedInUserService.resolve(principal);
        boolean isAdmin = user != null && (user.hasRole("ADMIN") || user.hasRole("SUPER_ADMIN"));
        EventPoojaUserRegistration created = service.createRegistration(registration, user, communityId, adminOverride && isAdmin);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping("/my")
    public ResponseEntity<List<EventPoojaUserRegistration>> getMyRegistrations(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestHeader(value = "X-Community-Id", required = false) Long communityId) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(service.getMyRegistrations(user, communityId));
    }

    @GetMapping
    public ResponseEntity<List<EventPoojaUserRegistration>> getAllRegistrations(
            @RequestHeader(value = "X-Community-Id", required = false) Long communityId) {
        return ResponseEntity.ok(service.getRegistrationsByCommunity(communityId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<EventPoojaUserRegistration> getRegistrationById(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(service.getRegistrationById(id, user));
    }

    @PutMapping("/{id}")
    public ResponseEntity<EventPoojaUserRegistration> updateRegistration(
            @PathVariable Long id,
            @RequestBody EventPoojaUserRegistration patch,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        EventPoojaUserRegistration updated = service.updateRegistration(id, patch, user);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> cancelOrDeleteRegistration(
            @PathVariable Long id,
            @RequestParam(value = "permanent", required = false, defaultValue = "false") boolean permanent,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        if (permanent) {
            service.deleteRegistration(id, user);
        } else {
            service.cancelRegistration(id, user);
        }
        return ResponseEntity.noContent().build();
    }
}
