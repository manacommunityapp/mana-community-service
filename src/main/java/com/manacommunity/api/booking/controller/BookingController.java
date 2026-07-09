package com.manacommunity.api.booking.controller;

import com.manacommunity.api.booking.dto.*;
import com.manacommunity.api.booking.service.BookingService;
import com.manacommunity.api.user.model.AppUser;
import com.manacommunity.api.user.security.UserPrincipal;
import com.manacommunity.api.user.service.LoggedInUserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/bookings")
@RequiredArgsConstructor
public class BookingController {

    private final BookingService bookingService;
    private final LoggedInUserService loggedInUserService;

    // ── Amenities ───────────────────────────────────────────────────

    @GetMapping("/amenities")
    @PreAuthorize("hasAuthority('View Amenities')")
    public ResponseEntity<List<AmenityResponse>> getAmenities(
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        Long communityId = user.getCommunity() != null ? user.getCommunity().getId() : null;
        if (communityId == null) return ResponseEntity.ok(List.of());
        return ResponseEntity.ok(bookingService.getAmenities(communityId));
    }

    @PostMapping("/amenities")
    @PreAuthorize("hasAuthority('Manage Amenities')")
    public ResponseEntity<AmenityResponse> createAmenity(
            @Valid @RequestBody AmenityRequest req,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(bookingService.createAmenity(req, user.getCommunity()));
    }

    @PutMapping("/amenities/{id}")
    @PreAuthorize("hasAuthority('Manage Amenities')")
    public ResponseEntity<AmenityResponse> updateAmenity(
            @PathVariable Long id,
            @Valid @RequestBody AmenityRequest req) {
        return ResponseEntity.ok(bookingService.updateAmenity(id, req));
    }

    // ── Bookings ────────────────────────────────────────────────────

    @GetMapping("/amenities/{amenityId}/slots")
    @PreAuthorize("hasAuthority('View Amenities')")
    public ResponseEntity<List<BookingResponse>> getSlots(
            @PathVariable Long amenityId,
            @RequestParam String date) {
        return ResponseEntity.ok(bookingService.getBookingsForDate(amenityId, LocalDate.parse(date)));
    }

    @GetMapping("/mine")
    @PreAuthorize("hasAuthority('Book Amenity')")
    public ResponseEntity<List<BookingResponse>> getMyBookings(
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(bookingService.getMyBookings(user.getId()));
    }

    @GetMapping("/today")
    @PreAuthorize("hasAuthority('Manage Amenities')")
    public ResponseEntity<List<BookingResponse>> getTodaysBookings(
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        Long communityId = user.getCommunity() != null ? user.getCommunity().getId() : null;
        if (communityId == null) return ResponseEntity.ok(List.of());
        return ResponseEntity.ok(bookingService.getTodaysBookings(communityId));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('Book Amenity')")
    public ResponseEntity<BookingResponse> createBooking(
            @Valid @RequestBody BookingRequest req,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(bookingService.createBooking(req, user));
    }

    @PutMapping("/{id}/cancel")
    @PreAuthorize("hasAuthority('Book Amenity')")
    public ResponseEntity<Void> cancelBooking(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        bookingService.cancelBooking(id, user.getId());
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('Manage Amenities')")
    public ResponseEntity<Void> adminCancel(@PathVariable Long id) {
        bookingService.adminCancel(id);
        return ResponseEntity.noContent().build();
    }
}
