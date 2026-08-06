package com.manacommunity.api.vendor.controller;

import com.manacommunity.api.user.model.AppUser;
import com.manacommunity.api.user.security.UserPrincipal;
import com.manacommunity.api.user.service.LoggedInUserService;
import com.manacommunity.api.vendor.dto.BookingRequest;
import com.manacommunity.api.vendor.dto.BookingResponse;
import com.manacommunity.api.vendor.service.VmsBookingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/vendor/bookings")
@RequiredArgsConstructor
public class VmsBookingController {

    private final VmsBookingService bookingService;
    private final LoggedInUserService loggedInUserService;

    @GetMapping
    @PreAuthorize("hasAuthority('View Vendor Management')")
    public ResponseEntity<Page<BookingResponse>> getBookings(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        PageRequest pageable = PageRequest.of(page, Math.min(size, 50), Sort.by(Sort.Direction.DESC, "createdAt"));
        return ResponseEntity.ok(bookingService.getCommunityBookings(user.getCommunity().getId(), status, pageable));
    }

    @GetMapping("/mine")
    @PreAuthorize("hasAuthority('Book Vendor Service')")
    public ResponseEntity<Page<BookingResponse>> getMyBookings(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        PageRequest pageable = PageRequest.of(page, Math.min(size, 50), Sort.by(Sort.Direction.DESC, "createdAt"));
        return ResponseEntity.ok(bookingService.getUserBookings(user.getId(), status, pageable));
    }

    @GetMapping("/vendor/{vendorId}")
    @PreAuthorize("hasAuthority('View Vendor Management')")
    public ResponseEntity<Page<BookingResponse>> getVendorBookings(
            @PathVariable Long vendorId,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        PageRequest pageable = PageRequest.of(page, Math.min(size, 50), Sort.by(Sort.Direction.DESC, "createdAt"));
        return ResponseEntity.ok(bookingService.getVendorBookings(vendorId, status, pageable));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('View Vendor Management')")
    public ResponseEntity<BookingResponse> getById(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(bookingService.getById(id, user.getCommunity().getId()));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('Book Vendor Service')")
    public ResponseEntity<BookingResponse> create(
            @Valid @RequestBody BookingRequest req,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(bookingService.create(req, user, user.getCommunity()));
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("hasAuthority('Manage Vendors')")
    public ResponseEntity<BookingResponse> updateStatus(
            @PathVariable Long id,
            @RequestParam String status,
            @RequestParam(required = false) String reason,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(bookingService.updateStatus(id, status, user.getCommunity().getId(), reason));
    }
}
