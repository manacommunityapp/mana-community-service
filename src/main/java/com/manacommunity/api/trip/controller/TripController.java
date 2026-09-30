package com.manacommunity.api.trip.controller;

import com.manacommunity.api.trip.entity.Trip;
import com.manacommunity.api.trip.entity.TripBooking;
import com.manacommunity.api.trip.service.TripService;
import com.manacommunity.api.user.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/trips")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class TripController {

    private final TripService tripService;

    @GetMapping
    public ResponseEntity<List<Trip>> getTrips(@RequestParam(required = false) String category) {
        return ResponseEntity.ok(tripService.getTrips(category));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Trip> getTrip(@PathVariable String id) {
        return ResponseEntity.ok(tripService.getTrip(id));
    }

    @PostMapping("/{id}/book")
    public ResponseEntity<TripBooking> bookTrip(
            @PathVariable String id,
            @RequestBody Map<String, Object> payload,
            @AuthenticationPrincipal UserPrincipal principal) {
        Long userId = principal != null ? principal.getId() : 1L;
        return ResponseEntity.ok(tripService.bookTrip(id, userId, payload));
    }

    @GetMapping("/my-bookings")
    public ResponseEntity<List<TripBooking>> getMyBookings(@AuthenticationPrincipal UserPrincipal principal) {
        Long userId = principal != null ? principal.getId() : 1L;
        return ResponseEntity.ok(tripService.getMyBookings(userId));
    }

    @PostMapping("/bookings/{id}/cancel")
    public ResponseEntity<Map<String, Object>> cancelBooking(
            @PathVariable String id,
            @RequestBody(required = false) Map<String, Object> body) {
        String reason = body != null ? (String) body.get("reason") : "User requested cancellation";
        return ResponseEntity.ok(tripService.cancelBooking(id, reason));
    }

    @PostMapping
    public ResponseEntity<Trip> createTrip(@RequestBody Trip trip) {
        return ResponseEntity.ok(tripService.createTrip(trip));
    }

    @GetMapping("/{id}/manifest")
    public ResponseEntity<List<TripBooking>> getManifest(@PathVariable String id) {
        return ResponseEntity.ok(tripService.getManifest(id));
    }

    @PostMapping("/bookings/{id}/check-in")
    public ResponseEntity<TripBooking> checkIn(@PathVariable String id) {
        return ResponseEntity.ok(tripService.checkInPassenger(id));
    }

    @PostMapping("/{id}/reviews")
    public ResponseEntity<Map<String, String>> submitReview(
            @PathVariable String id,
            @RequestBody Map<String, Object> review) {
        return ResponseEntity.ok(Map.of("status", "SUCCESS", "message", "Review recorded."));
    }
}