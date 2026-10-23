package com.manacommunity.api.booking.controller;

import com.manacommunity.api.booking.dto.ResourceBookingRequest;
import com.manacommunity.api.booking.service.ResourceBookingService;
import com.manacommunity.api.booking.service.ResourceService;
import com.manacommunity.api.user.model.AppUser;
import com.manacommunity.api.user.security.UserPrincipal;
import com.manacommunity.api.user.service.LoggedInUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/facilities")
@RequiredArgsConstructor
public class FacilityAliasController {

    private final ResourceService resourceService;
    private final ResourceBookingService bookingService;
    private final LoggedInUserService loggedInUserService;

    @GetMapping
    public ResponseEntity<?> listFacilities(
            @RequestParam(required = false) Long categoryId,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        Long communityId = user.getCommunity().getId();
        return ResponseEntity.ok(resourceService.getResources(communityId, categoryId));
    }

    @GetMapping("/{id}/slots")
    public ResponseEntity<?> getSlots(
            @PathVariable Long id,
            @RequestParam String date,
            @AuthenticationPrincipal UserPrincipal principal) {
        loggedInUserService.resolve(principal);
        LocalDate d = LocalDate.parse(date);
        return ResponseEntity.ok(bookingService.generateSlots(id, d));
    }

    @PostMapping("/book")
    public ResponseEntity<?> bookFacility(
            @RequestBody ResourceBookingRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(bookingService.createBooking(request, user, user.getCommunity()));
    }

    @GetMapping("/my-bookings")
    public ResponseEntity<?> myBookings(
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(bookingService.getMyBookings(user.getId(), null));
    }
}
