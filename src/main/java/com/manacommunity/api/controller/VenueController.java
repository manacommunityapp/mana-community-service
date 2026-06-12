package com.manacommunity.api.controller;

import com.manacommunity.api.constants.PermissionConstants;
import com.manacommunity.api.model.AppUser;
import com.manacommunity.api.model.Venue;
import com.manacommunity.api.security.UserPrincipal;
import com.manacommunity.api.service.LoggedInUserService;
import com.manacommunity.api.service.PermissionCheckService;
import com.manacommunity.api.service.VenueService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Objects;

@RestController
@RequestMapping("/api/venues")
@RequiredArgsConstructor
public class VenueController {

    private final VenueService venueService;
    private final LoggedInUserService loggedInUserService;
    private final PermissionCheckService permissionCheckService;

    @GetMapping
    public ResponseEntity<List<Venue>> getVenues(
            @RequestParam(required = false) Long communityId,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser loggedInUser = loggedInUserService.resolve(principal);
        boolean isSuperAdmin = "SUPER_ADMIN".equals(loggedInUser.getRole());
        Long targetCommunityId = communityId;
        if (!isSuperAdmin) {
            targetCommunityId = loggedInUser.getCommunity() != null ? loggedInUser.getCommunity().getId() : null;
            if (targetCommunityId == null) {
                return ResponseEntity.ok(java.util.Collections.emptyList());
            }
        }
        if (targetCommunityId == null) {
            return ResponseEntity.ok(venueService.getAllVenues());
        }
        return ResponseEntity.ok(venueService.getVenuesByCommunityId(targetCommunityId));
    }

    @PostMapping
    public ResponseEntity<Venue> createVenue(
            @RequestParam(required = false) Long communityId,
            @RequestBody Venue venue,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser loggedInUser = loggedInUserService.resolve(principal);
        Long targetCommunityId = communityId;
        if (!"SUPER_ADMIN".equals(loggedInUser.getRole())) {
            targetCommunityId = loggedInUser.getCommunity() != null ? loggedInUser.getCommunity().getId() : null;
        }
        return ResponseEntity.ok(venueService.createVenue(targetCommunityId, venue));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Venue> updateVenue(
            @PathVariable Long id,
            @RequestBody Venue venue,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser loggedInUser = loggedInUserService.resolve(principal);
        
        // Check if timing is being updated, if so enforce role permission check
        Venue existing = venueService.getVenueById(id);
        boolean timingChanged = !Objects.equals(existing.getOpeningTime(), venue.getOpeningTime())
                || !Objects.equals(existing.getClosingTime(), venue.getClosingTime());
        if (timingChanged) {
            permissionCheckService.requireAnyPermission(principal, PermissionConstants.EDIT_VENUE_TIMING);
        }
        
        return ResponseEntity.ok(venueService.updateVenue(id, venue));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteVenue(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser loggedInUser = loggedInUserService.resolve(principal);
        venueService.deleteVenue(id);
        return ResponseEntity.noContent().build();
    }
}
