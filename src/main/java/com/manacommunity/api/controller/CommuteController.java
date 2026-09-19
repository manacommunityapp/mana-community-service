package com.manacommunity.api.controller;

import com.manacommunity.api.dto.*;
import com.manacommunity.api.user.model.AppUser;
import com.manacommunity.api.user.security.UserPrincipal;
import com.manacommunity.api.service.CommuteService;
import com.manacommunity.api.user.service.LoggedInUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/commute")
@RequiredArgsConstructor
public class CommuteController {

    private final LoggedInUserService loggedInUserService;
    private final CommuteService commuteService;

    @GetMapping("/rides")
    public ResponseEntity<Page<CommuteRideResponse>> getUpcomingRides(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false) String rideType,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        AppUser user = loggedInUserService.resolve(principal);
        int safeSize = Math.min(Math.max(size, 1), 50);
        return ResponseEntity.ok(commuteService.getUpcomingRides(user, rideType, Math.max(page, 0), safeSize));
    }

    @GetMapping("/rides/search")
    public ResponseEntity<Page<CommuteRideResponse>> searchRides(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam String destination,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        AppUser user = loggedInUserService.resolve(principal);
        int safeSize = Math.min(Math.max(size, 1), 50);
        return ResponseEntity.ok(commuteService.searchRides(user, destination, Math.max(page, 0), safeSize));
    }

    @GetMapping("/rides/{id}")
    public ResponseEntity<CommuteRideResponse> getRide(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(commuteService.getRide(user, id));
    }

    @PostMapping("/rides")
    public ResponseEntity<CommuteRideResponse> createRide(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody CreateCommuteRideRequest request) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(commuteService.createRide(user, request));
    }

    @PutMapping("/rides/{id}")
    public ResponseEntity<CommuteRideResponse> updateRide(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id,
            @Valid @RequestBody CreateCommuteRideRequest request) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(commuteService.updateRide(user, id, request));
    }

    @DeleteMapping("/rides/{id}")
    public ResponseEntity<Void> cancelRide(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id) {
        AppUser user = loggedInUserService.resolve(principal);
        commuteService.cancelRide(user, id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/rides/my-offers")
    public ResponseEntity<Page<CommuteRideResponse>> getMyOfferedRides(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        AppUser user = loggedInUserService.resolve(principal);
        int safeSize = Math.min(Math.max(size, 1), 50);
        return ResponseEntity.ok(commuteService.getMyOfferedRides(user, Math.max(page, 0), safeSize));
    }

    @GetMapping("/rides/my-bookings")
    public ResponseEntity<Page<CommuteRideResponse>> getMyBookedRides(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        AppUser user = loggedInUserService.resolve(principal);
        int safeSize = Math.min(Math.max(size, 1), 50);
        return ResponseEntity.ok(commuteService.getMyBookedRides(user, Math.max(page, 0), safeSize));
    }

    @PostMapping("/rides/{id}/book")
    public ResponseEntity<CommuteBookingResponse> bookRide(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id,
            @Valid @RequestBody CreateCommuteBookingRequest request) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(commuteService.bookRide(user, id, request));
    }

    @DeleteMapping("/rides/{id}/book")
    public ResponseEntity<Void> cancelBooking(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id) {
        AppUser user = loggedInUserService.resolve(principal);
        commuteService.cancelBooking(user, id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/bookings/{id}/confirm")
    public ResponseEntity<CommuteBookingResponse> confirmBooking(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(commuteService.confirmBooking(user, id));
    }

    @PatchMapping("/bookings/{id}/reject")
    public ResponseEntity<CommuteBookingResponse> rejectBooking(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(commuteService.rejectBooking(user, id));
    }

    @GetMapping("/stats")
    public ResponseEntity<CommuteStatsResponse> getStats(
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(commuteService.getStats(user));
    }
}
