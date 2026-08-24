package com.manacommunity.api.events.controller;

import com.manacommunity.api.events.entity.EventBookingRegistration;
import com.manacommunity.api.events.service.EventBookingRegistrationService;
import com.manacommunity.api.user.model.AppUser;
import com.manacommunity.api.user.security.UserPrincipal;
import com.manacommunity.api.user.service.LoggedInUserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/events/registrations")
public class EventBookingRegistrationController {

    private final EventBookingRegistrationService service;
    private final LoggedInUserService loggedInUserService;

    public EventBookingRegistrationController(EventBookingRegistrationService service, LoggedInUserService loggedInUserService) {
        this.service = service;
        this.loggedInUserService = loggedInUserService;
    }

    @PostMapping
    public ResponseEntity<EventBookingRegistration> createRegistration(
            @RequestBody EventBookingRegistration registration,
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestHeader(value = "X-Community-Id", required = false) Long communityId,
            @RequestParam(value = "adminOverride", required = false, defaultValue = "false") boolean adminOverride) {
        AppUser user = loggedInUserService.resolve(principal);
        boolean isAdmin = user != null && (user.hasRole("ADMIN") || user.hasRole("SUPER_ADMIN"));
        EventBookingRegistration created = service.createRegistration(registration, user, communityId, adminOverride && isAdmin);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping("/my")
    public ResponseEntity<List<EventBookingRegistration>> getMyRegistrations(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestHeader(value = "X-Community-Id", required = false) Long communityId,
            @RequestParam(value = "status", required = false) String status) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(service.getMyRegistrations(user, communityId, status));
    }

    @GetMapping
    public ResponseEntity<List<EventBookingRegistration>> getAllRegistrations(
            @RequestHeader(value = "X-Community-Id", required = false) Long communityId,
            @RequestParam(value = "status", required = false) String status) {
        return ResponseEntity.ok(service.getRegistrationsByCommunity(communityId, status));
    }

    @GetMapping("/{id}")
    public ResponseEntity<EventBookingRegistration> getRegistrationById(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(service.getRegistrationById(id, user));
    }

    @PutMapping("/{id}")
    public ResponseEntity<EventBookingRegistration> updateRegistration(
            @PathVariable Long id,
            @RequestBody EventBookingRegistration patch,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        EventBookingRegistration updated = service.updateRegistration(id, patch, user);
        return ResponseEntity.ok(updated);
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<Void> cancelRegistration(
            @PathVariable Long id,
            @RequestParam(value = "reason", required = false) String reason,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        service.cancelRegistration(id, reason, user);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> cancelOrDeleteRegistration(
            @PathVariable Long id,
            @RequestParam(value = "permanent", required = false, defaultValue = "false") boolean permanent,
            @RequestParam(value = "reason", required = false) String reason,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        if (permanent) {
            service.deleteRegistration(id, user);
        } else {
            service.cancelRegistration(id, reason, user);
        }
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}/permanent")
    public ResponseEntity<Void> permanentlyDeleteRegistration(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        service.deleteRegistration(id, user);
        return ResponseEntity.noContent().build();
    }
}
