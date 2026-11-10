package com.manacommunity.api.homeservices.controller;

import com.manacommunity.api.homeservices.dto.BookServiceRequest;
import com.manacommunity.api.homeservices.dto.CompleteBookingRequest;
import com.manacommunity.api.homeservices.entity.HomeServiceBooking;
import com.manacommunity.api.homeservices.entity.HomeServiceCategory;
import com.manacommunity.api.homeservices.entity.HomeServiceProvider;
import com.manacommunity.api.homeservices.service.HomeServiceMarketplaceService;
import com.manacommunity.api.user.model.AppUser;
import com.manacommunity.api.user.security.UserPrincipal;
import com.manacommunity.api.user.service.LoggedInUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/home-services")
@RequiredArgsConstructor
public class HomeServiceMarketplaceController {

    private final HomeServiceMarketplaceService service;
    private final LoggedInUserService loggedInUserService;

    @GetMapping("/categories")
    public ResponseEntity<List<HomeServiceCategory>> getCategories() {
        return ResponseEntity.ok(service.getCategories());
    }

    @GetMapping("/providers")
    public ResponseEntity<List<HomeServiceProvider>> getProviders(
            @RequestParam Long communityId,
            @RequestParam(required = false) Long categoryId) {
        return ResponseEntity.ok(service.getProviders(communityId, categoryId));
    }

    @GetMapping("/providers/{id}")
    public ResponseEntity<HomeServiceProvider> getProvider(@PathVariable Long id) {
        return ResponseEntity.ok(service.getProviderById(id));
    }

    @PostMapping("/bookings")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<HomeServiceBooking> bookService(
            @RequestBody BookServiceRequest req,
            @RequestParam Long communityId,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        HomeServiceBooking booking = service.bookService(
                communityId, user.getId(), req.getProviderId(),
                req.getScheduledDate(), req.getScheduledTime(),
                req.getDescription(), req.getFlatNo() != null ? req.getFlatNo() : user.getFlatNo());
        return ResponseEntity.ok(booking);
    }

    @GetMapping("/bookings")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<HomeServiceBooking>> getAllBookings(
            @RequestParam Long communityId,
            @RequestParam(required = false) String status) {
        return ResponseEntity.ok(service.getAllBookings(communityId, status));
    }

    @GetMapping("/bookings/my")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<HomeServiceBooking>> getMyBookings(
            @RequestParam Long communityId,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(service.getResidentBookings(communityId, user.getId()));
    }

    @PutMapping("/bookings/{id}/status")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<HomeServiceBooking> updateStatus(
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {
        return ResponseEntity.ok(service.updateBookingStatus(id, body.get("status")));
    }

    @PostMapping("/bookings/{id}/complete")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<HomeServiceBooking> completeBooking(
            @PathVariable Long id,
            @RequestBody CompleteBookingRequest req,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(service.completeBookingWithRating(
                id, user.getId(), req.getRating(), req.getReview(), req.getAmount()));
    }

    @PutMapping("/bookings/{id}/cancel")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<HomeServiceBooking> cancelBooking(
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {
        return ResponseEntity.ok(service.cancelBooking(id, body.get("reason")));
    }
}
