package com.manacommunity.api.trip.controller;

import com.manacommunity.api.trip.dto.TripRequest;
import com.manacommunity.api.trip.dto.TripResponse;
import com.manacommunity.api.trip.service.TripService;
import com.manacommunity.api.user.model.AppUser;
import com.manacommunity.api.user.security.UserPrincipal;
import com.manacommunity.api.user.service.LoggedInUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping({"/api/trips", "/trips"})
@RequiredArgsConstructor
@Tag(name = "Trips", description = "Community trip booking APIs")
public class TripController {

    private final TripService tripService;
    private final LoggedInUserService loggedInUserService;

    @GetMapping
    @Operation(summary = "Get all trips for the community")
    public ResponseEntity<List<TripResponse>> getTrips(
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(tripService.getTrips(user.getCommunity().getId()));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a single trip by ID")
    public ResponseEntity<TripResponse> getTrip(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(tripService.getTrip(id, user.getCommunity().getId()));
    }

    @PostMapping
    @Operation(summary = "Create a new trip")
    public ResponseEntity<TripResponse> createTrip(
            @Valid @RequestBody TripRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(tripService.createTrip(request, user));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('View Admin')")
    @Operation(summary = "Update a trip")
    public ResponseEntity<TripResponse> updateTrip(
            @PathVariable Long id,
            @Valid @RequestBody TripRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(tripService.updateTrip(id, request, user.getCommunity().getId()));
    }

    @PatchMapping("/{id}/cancel")
    @PreAuthorize("hasAuthority('View Admin')")
    @Operation(summary = "Cancel a trip")
    public ResponseEntity<Void> cancelTrip(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        tripService.cancelTrip(id, user.getCommunity().getId());
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('View Admin')")
    @Operation(summary = "Delete a trip")
    public ResponseEntity<Void> deleteTrip(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        tripService.deleteTrip(id, user.getCommunity().getId());
        return ResponseEntity.ok().build();
    }
}
