package com.manacommunity.api.homeservices.instahelp.controller;

import com.manacommunity.api.homeservices.instahelp.dto.InstaHelpRequest;
import com.manacommunity.api.homeservices.instahelp.dto.InstaHelpResponse;
import com.manacommunity.api.homeservices.instahelp.dto.RateRequest;
import com.manacommunity.api.homeservices.instahelp.service.InstaHelpService;
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

@RestController
@RequestMapping("/api/v1/insta-help")
@RequiredArgsConstructor
public class InstaHelpController {

    private final InstaHelpService instaHelpService;
    private final LoggedInUserService loggedInUserService;

    @PostMapping("/bookings")
    public ResponseEntity<InstaHelpResponse> createBooking(
            @Valid @RequestBody InstaHelpRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        Long communityId = user.getCommunity() != null ? user.getCommunity().getId() : null;
        if (communityId == null) return ResponseEntity.badRequest().build();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(instaHelpService.create(communityId, user.getId(), request));
    }

    @GetMapping("/bookings/my")
    public ResponseEntity<List<InstaHelpResponse>> getMyBookings(
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(instaHelpService.getMyBookings(user.getId()));
    }

    @PutMapping("/bookings/{ref}/start")
    public ResponseEntity<InstaHelpResponse> startSession(
            @PathVariable String ref,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(instaHelpService.startSession(ref, user.getId()));
    }

    @PutMapping("/bookings/{ref}/complete")
    public ResponseEntity<InstaHelpResponse> completeSession(
            @PathVariable String ref,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(instaHelpService.completeSession(ref, user.getId()));
    }

    @PutMapping("/bookings/{ref}/cancel")
    public ResponseEntity<InstaHelpResponse> cancelBooking(
            @PathVariable String ref,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(instaHelpService.cancel(ref, user.getId()));
    }

    @PutMapping("/bookings/{ref}/rate")
    public ResponseEntity<InstaHelpResponse> rateBooking(
            @PathVariable String ref,
            @Valid @RequestBody RateRequest rateRequest,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(instaHelpService.rate(ref, user.getId(), rateRequest));
    }
}
