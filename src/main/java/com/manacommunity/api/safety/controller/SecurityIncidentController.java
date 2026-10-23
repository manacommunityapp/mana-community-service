package com.manacommunity.api.safety.controller;

import com.manacommunity.api.safety.dto.CreateIncidentRequest;
import com.manacommunity.api.safety.dto.SecurityIncidentResponse;
import com.manacommunity.api.safety.service.SecurityIncidentService;
import com.manacommunity.api.user.model.AppUser;
import com.manacommunity.api.user.security.UserPrincipal;
import com.manacommunity.api.user.service.LoggedInUserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping({"/api/guard/incidents", "/api/security/incidents"})
@RequiredArgsConstructor
public class SecurityIncidentController {

    private final SecurityIncidentService service;
    private final LoggedInUserService loggedInUserService;

    @GetMapping
    public ResponseEntity<List<SecurityIncidentResponse>> list(
            @RequestParam(required = false) String status,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        Long communityId = user.getCommunity() != null ? user.getCommunity().getId() : null;
        if (communityId == null) return ResponseEntity.ok(List.of());
        return ResponseEntity.ok(service.getIncidents(communityId, status));
    }

    @PostMapping
    public ResponseEntity<SecurityIncidentResponse> create(
            @Valid @RequestBody CreateIncidentRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        Long communityId = user.getCommunity() != null ? user.getCommunity().getId() : null;
        if (communityId == null) return ResponseEntity.badRequest().build();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(service.createIncident(communityId, request, user));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<SecurityIncidentResponse> updateStatus(
            @PathVariable Long id,
            @RequestBody Map<String, String> body,
            @AuthenticationPrincipal UserPrincipal principal) {
        loggedInUserService.resolve(principal);
        return ResponseEntity.ok(service.updateIncidentStatus(
                id, body.get("status"), body.get("resolutionNotes")));
    }
}
