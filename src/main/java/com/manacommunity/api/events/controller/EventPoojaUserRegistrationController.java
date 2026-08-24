package com.manacommunity.api.events.controller;

import com.manacommunity.api.events.dto.PoojaRescheduleRequest;
import com.manacommunity.api.events.entity.EventPoojaUserRegistration;
import com.manacommunity.api.events.service.EventPoojaUserRegistrationService;
import com.manacommunity.api.exception.ResourceNotFoundException;
import jakarta.validation.Valid;
import com.manacommunity.api.user.model.AppUser;
import com.manacommunity.api.user.repository.AppUserRepository;
import com.manacommunity.api.user.security.UserPrincipal;
import com.manacommunity.api.user.service.LoggedInUserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/events/pooja-registrations")
public class EventPoojaUserRegistrationController {

    private final EventPoojaUserRegistrationService service;
    private final LoggedInUserService loggedInUserService;
    private final AppUserRepository userRepository;

    public EventPoojaUserRegistrationController(
            EventPoojaUserRegistrationService service,
            LoggedInUserService loggedInUserService,
            AppUserRepository userRepository) {
        this.service = service;
        this.loggedInUserService = loggedInUserService;
        this.userRepository = userRepository;
    }

    @PostMapping
    public ResponseEntity<EventPoojaUserRegistration> createRegistration(
            @RequestBody EventPoojaUserRegistration registration,
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestHeader(value = "X-Community-Id", required = false) Long communityId,
            @RequestParam(value = "adminOverride", required = false, defaultValue = "false") boolean adminOverride,
            @RequestParam(value = "targetUserId", required = false) Long targetUserId) {
        AppUser caller = loggedInUserService.resolve(principal);
        if (communityId == null && principal != null && principal.getCommunityId() != null) {
            communityId = principal.getCommunityId();
        }
        if (communityId == null && caller != null && caller.getCommunity() != null) {
            communityId = caller.getCommunity().getId();
        }
        boolean isAdmin = caller != null && (
                caller.hasRole("ADMIN") || caller.hasRole("SUPER_ADMIN") ||
                caller.hasRole("COMMUNITY_ADMIN") || caller.hasRole("EVENT_ADMIN") ||
                caller.hasRole("ROLE_ADMIN") || caller.hasRole("ROLE_SUPER_ADMIN") ||
                caller.hasRole("ROLE_COMMUNITY_ADMIN") || caller.hasRole("ROLE_EVENT_ADMIN"));
        // #3: When admin registers on behalf of a member, resolve the target user
        AppUser effectiveUser = caller;
        if (adminOverride && isAdmin && targetUserId != null) {
            effectiveUser = userRepository.findById(targetUserId)
                    .orElseThrow(() -> new ResourceNotFoundException("AppUser", targetUserId));
        }
        EventPoojaUserRegistration created = service.createRegistration(registration, effectiveUser, communityId, adminOverride && isAdmin);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','COMMUNITY_ADMIN','EVENT_ADMIN','SUPER_ADMIN') or hasAuthority('Manage Event Forms')")
    public ResponseEntity<List<EventPoojaUserRegistration>> getAllRegistrations(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestHeader(value = "X-Community-Id", required = false) Long communityId) {
        if (communityId == null && principal != null && principal.getCommunityId() != null) {
            communityId = principal.getCommunityId();
        }
        if (communityId == null) {
            AppUser user = loggedInUserService.resolve(principal);
            if (user != null && user.getCommunity() != null) {
                communityId = user.getCommunity().getId();
            }
        }
        return ResponseEntity.ok(service.getRegistrationsByCommunity(communityId));
    }

    @GetMapping("/my")
    public ResponseEntity<List<EventPoojaUserRegistration>> getMyRegistrations(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestHeader(value = "X-Community-Id", required = false) Long communityId) {
        AppUser user = loggedInUserService.resolve(principal);
        if (communityId == null && principal != null && principal.getCommunityId() != null) {
            communityId = principal.getCommunityId();
        }
        if (communityId == null && user != null && user.getCommunity() != null) {
            communityId = user.getCommunity().getId();
        }
        return ResponseEntity.ok(service.getMyRegistrations(user, communityId));
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

    @PostMapping("/{id}/reschedule")
    public ResponseEntity<EventPoojaUserRegistration> rescheduleRegistration(
            @PathVariable Long id,
            @RequestBody @Valid PoojaRescheduleRequest req,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        EventPoojaUserRegistration updated = service.reschedule(id, req.getNewScheduleId(), req.getIdempotencyKey(), user);
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
