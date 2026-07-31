package com.manacommunity.api.food.controller;

import com.manacommunity.api.user.model.AppUser;
import com.manacommunity.api.user.security.UserPrincipal;
import com.manacommunity.api.user.service.LoggedInUserService;
import com.manacommunity.api.food.service.FoodDiningReservationService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Map;

@RestController
@RequestMapping("/api/food/dining")
@RequiredArgsConstructor
public class FoodDiningController {

    private final FoodDiningReservationService diningReservationService;
    private final LoggedInUserService loggedInUserService;

    @PostMapping("/reservations")
    @PreAuthorize("hasAuthority('Manage Food Dining')")
    public ResponseEntity<?> create(
            @Valid @RequestBody Map<String, Object> request,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        Long communityId = user.getCommunity().getId();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(diningReservationService.create(communityId, request, user));
    }

    @GetMapping("/reservations")
    @PreAuthorize("hasAuthority('View Food Dining')")
    public ResponseEntity<?> getMyReservations(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        Long communityId = user.getCommunity().getId();
        PageRequest pageable = PageRequest.of(page, Math.min(size, 50), Sort.by(Sort.Direction.DESC, "createdAt"));
        return ResponseEntity.ok(diningReservationService.getMyReservations(communityId, user.getId(), pageable));
    }

    @GetMapping("/reservations/{id}")
    @PreAuthorize("hasAuthority('View Food Dining')")
    public ResponseEntity<?> getById(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        Long communityId = user.getCommunity().getId();
        return ResponseEntity.ok(diningReservationService.getById(communityId, id));
    }

    @GetMapping("/reservations/restaurant/{restaurantId}")
    @PreAuthorize("hasAuthority('View Food Dining')")
    public ResponseEntity<?> getRestaurantReservations(
            @PathVariable Long restaurantId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) String status,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        Long communityId = user.getCommunity().getId();
        return ResponseEntity.ok(diningReservationService.getRestaurantReservations(communityId, restaurantId, date, status));
    }

    @PatchMapping("/reservations/{id}/status")
    @PreAuthorize("hasAuthority('Manage Food Dining')")
    public ResponseEntity<?> updateStatus(
            @PathVariable Long id,
            @RequestParam String status,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        Long communityId = user.getCommunity().getId();
        return ResponseEntity.ok(diningReservationService.updateStatus(communityId, id, status));
    }

    @PostMapping("/reservations/{id}/check-in")
    @PreAuthorize("hasAuthority('Manage Food Dining')")
    public ResponseEntity<?> checkIn(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        Long communityId = user.getCommunity().getId();
        return ResponseEntity.ok(diningReservationService.checkIn(communityId, id));
    }

    @PostMapping("/reservations/{id}/cancel")
    @PreAuthorize("hasAuthority('Manage Food Dining')")
    public ResponseEntity<?> cancel(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        Long communityId = user.getCommunity().getId();
        return ResponseEntity.ok(diningReservationService.cancel(communityId, id));
    }

    @PostMapping("/waitlist")
    @PreAuthorize("hasAuthority('Manage Food Dining')")
    public ResponseEntity<?> joinWaitlist(
            @Valid @RequestBody Map<String, Object> request,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        Long communityId = user.getCommunity().getId();
        return ResponseEntity.ok(diningReservationService.joinWaitlist(communityId, request, user));
    }

    @GetMapping("/waitlist/{restaurantId}")
    @PreAuthorize("hasAuthority('View Food Dining')")
    public ResponseEntity<?> getWaitlist(
            @PathVariable Long restaurantId,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        Long communityId = user.getCommunity().getId();
        return ResponseEntity.ok(diningReservationService.getWaitlist(communityId, restaurantId));
    }

    @GetMapping("/events")
    @PreAuthorize("hasAuthority('View Food Dining')")
    public ResponseEntity<?> getDiningEvents(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        Long communityId = user.getCommunity().getId();
        PageRequest pageable = PageRequest.of(page, Math.min(size, 50), Sort.by(Sort.Direction.DESC, "createdAt"));
        return ResponseEntity.ok(diningReservationService.getDiningEvents(communityId, pageable));
    }

    @PostMapping("/reservations/{id}/feedback")
    @PreAuthorize("hasAuthority('Manage Food Dining')")
    public ResponseEntity<?> submitFeedback(
            @PathVariable Long id,
            @Valid @RequestBody Map<String, Object> request,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        Long communityId = user.getCommunity().getId();
        return ResponseEntity.ok(diningReservationService.submitFeedback(communityId, id, request, user));
    }
}
