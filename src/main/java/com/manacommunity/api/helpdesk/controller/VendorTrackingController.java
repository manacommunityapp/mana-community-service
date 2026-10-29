package com.manacommunity.api.helpdesk.controller;

import com.manacommunity.api.helpdesk.entity.VendorTrackingSession;
import com.manacommunity.api.helpdesk.repository.VendorTrackingSessionRepository;
import com.manacommunity.api.user.model.AppUser;
import com.manacommunity.api.user.security.UserPrincipal;
import com.manacommunity.api.user.service.LoggedInUserService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

// TODO: Integrate with real-time GPS push (WebSocket or SSE) for live tracking.
@RestController
@RequestMapping("/api/helpdesk/tracking")
@RequiredArgsConstructor
public class VendorTrackingController {

    private final VendorTrackingSessionRepository repo;
    private final LoggedInUserService loggedInUserService;

    /**
     * Returns the vendor's last known location for a booking.
     * Residents use this to see where the technician is.
     */
    @GetMapping("/{bookingId}")
    public ResponseEntity<Map<String, Object>> getLastLocation(@PathVariable Long bookingId) {
        return repo.findLatestByBookingId(bookingId)
                .map(t -> ResponseEntity.ok(Map.<String, Object>of(
                        "latitude", t.getLatitude(),
                        "longitude", t.getLongitude(),
                        "accuracy", t.getAccuracy() != null ? t.getAccuracy() : 0.0,
                        "recordedAt", t.getRecordedAt().toString()
                )))
                .orElse(ResponseEntity.ok(Map.of("message", "No location data yet")));
    }

    /**
     * Vendor/technician pushes their current GPS coordinates.
     * TODO: Add authentication to verify caller is the assigned vendor.
     */
    @PostMapping("/update")
    public ResponseEntity<VendorTrackingSession> updateLocation(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestBody LocationUpdateRequest request) {
        AppUser caller = loggedInUserService.resolve(principal);
        VendorTrackingSession session = repo.save(VendorTrackingSession.builder()
                .communityId(caller.getCommunity() != null ? caller.getCommunity().getId() : 0L)
                .bookingId(request.getBookingId())
                .vendorId(caller.getId())
                .latitude(request.getLatitude())
                .longitude(request.getLongitude())
                .accuracy(request.getAccuracy())
                .build());
        return ResponseEntity.ok(session);
    }

    @Data
    public static class LocationUpdateRequest {
        private Long bookingId;
        private Double latitude;
        private Double longitude;
        private Double accuracy;
    }
}
