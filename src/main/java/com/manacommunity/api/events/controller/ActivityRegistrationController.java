package com.manacommunity.api.events.controller;

import com.manacommunity.api.events.dto.ActivityRegistrationRequest;
import com.manacommunity.api.events.dto.ActivityRegistrationResponse;
import com.manacommunity.api.events.service.ActivityRegistrationService;
import com.manacommunity.api.user.model.AppUser;
import com.manacommunity.api.user.security.UserPrincipal;
import com.manacommunity.api.user.service.LoggedInUserService;
import jakarta.validation.Valid;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/events")
@RequiredArgsConstructor
public class ActivityRegistrationController {

    private final ActivityRegistrationService registrationService;
    private final LoggedInUserService loggedInUserService;

    @PostMapping("/programs/{programId}/registrations")
    @PreAuthorize("hasAuthority('Register Event')")
    public ResponseEntity<ActivityRegistrationResponse> register(
            @PathVariable Long programId,
            @Valid @RequestBody ActivityRegistrationRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(registrationService.register(programId, request, user));
    }

    @GetMapping("/programs/{programId}/registrations")
    @PreAuthorize("hasAuthority('View Events')")
    public ResponseEntity<List<ActivityRegistrationResponse>> getByProgram(@PathVariable Long programId) {
        return ResponseEntity.ok(registrationService.getByProgram(programId));
    }

    @GetMapping("/activity-registrations/mine")
    @PreAuthorize("hasAuthority('Register Event')")
    public ResponseEntity<List<ActivityRegistrationResponse>> getMine(
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(registrationService.getMine(user.getId()));
    }

    @PutMapping("/activity-registrations/{registrationId}/approve")
    @PreAuthorize("hasAuthority('Create Event')")
    public ResponseEntity<ActivityRegistrationResponse> approve(
            @PathVariable Long registrationId,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(registrationService.approve(registrationId, user));
    }

    @PutMapping("/activity-registrations/{registrationId}/reject")
    @PreAuthorize("hasAuthority('Create Event')")
    public ResponseEntity<ActivityRegistrationResponse> reject(
            @PathVariable Long registrationId,
            @RequestBody(required = false) DecisionRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(registrationService.reject(
                registrationId,
                request != null ? request.getReason() : null,
                user));
    }

    @PutMapping("/activity-registrations/{registrationId}/cancel")
    @PreAuthorize("hasAuthority('Register Event')")
    public ResponseEntity<ActivityRegistrationResponse> cancel(
            @PathVariable Long registrationId,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(registrationService.cancel(registrationId, user));
    }

    @Data
    public static class DecisionRequest {
        private String reason;
    }
}
