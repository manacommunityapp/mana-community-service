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
            @RequestHeader(value = "X-Community-Id", required = false) Long communityId) {
        AppUser user = loggedInUserService.resolve(principal);
        EventBookingRegistration created = service.createRegistration(registration, user, communityId);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping("/my")
    public ResponseEntity<List<EventBookingRegistration>> getMyRegistrations(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestHeader(value = "X-Community-Id", required = false) Long communityId) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(service.getMyRegistrations(user, communityId));
    }

    @GetMapping
    public ResponseEntity<List<EventBookingRegistration>> getAllRegistrations(
            @RequestHeader(value = "X-Community-Id", required = false) Long communityId) {
        return ResponseEntity.ok(service.getRegistrationsByCommunity(communityId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<EventBookingRegistration> getRegistrationById(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(service.getRegistrationById(id, user));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> cancelRegistration(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        service.cancelRegistration(id, user);
        return ResponseEntity.noContent().build();
    }
}
