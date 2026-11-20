package com.manacommunity.api.parking.controller;

import com.manacommunity.api.parking.entity.ParkingSession;
import com.manacommunity.api.parking.entity.ParkingSlotBooking;
import com.manacommunity.api.parking.service.ParkingSessionService;
import com.manacommunity.api.user.security.UserPrincipal;
import com.manacommunity.api.platform.web.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Tag(name = "Parking Sessions")
@RestController
@RequestMapping("/api/parking")
@RequiredArgsConstructor
public class ParkingSessionController {

    private final ParkingSessionService sessionService;

    @PostMapping("/sessions/check-in")
    public ResponseEntity<ApiResponse<ParkingSession>> checkIn(
            @RequestBody CheckInRequest req,
            @AuthenticationPrincipal UserPrincipal principal) {
        ParkingSession session = sessionService.checkIn(
                req.communityId, req.slotId, req.vehicleNumber,
                req.sessionType != null ? req.sessionType : ParkingSession.SessionType.VISITOR,
                principal.getId());
        return ResponseEntity.ok(ApiResponse.ok(session));
    }

    @PostMapping("/sessions/{sessionId}/check-out")
    public ResponseEntity<ApiResponse<ParkingSession>> checkOut(
            @PathVariable Long sessionId,
            @RequestParam Long communityId,
            @AuthenticationPrincipal UserPrincipal principal) {
        ParkingSession session = sessionService.checkOut(sessionId, communityId);
        return ResponseEntity.ok(ApiResponse.ok(session));
    }

    @GetMapping("/sessions/active")
    public ResponseEntity<ApiResponse<List<ParkingSession>>> getActiveSessions(
            @RequestParam Long communityId,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.ok(sessionService.getActiveSessions(communityId)));
    }

    @PostMapping("/bookings")
    public ResponseEntity<ApiResponse<ParkingSlotBooking>> bookVisitorParking(
            @RequestBody SlotBookingRequest req,
            @AuthenticationPrincipal UserPrincipal principal) {
        ParkingSlotBooking booking = sessionService.bookVisitorParking(
                req.communityId, principal.getId(), req.slotId,
                req.guestName, req.guestVehicle,
                req.bookingDate, req.startTime, req.endTime);
        return ResponseEntity.ok(ApiResponse.ok(booking));
    }

    @DeleteMapping("/bookings/{bookingId}")
    public ResponseEntity<ApiResponse<Void>> cancelBooking(
            @PathVariable Long bookingId,
            @AuthenticationPrincipal UserPrincipal principal) {
        sessionService.cancelBooking(bookingId, principal.getId());
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    @GetMapping("/bookings/my")
    public ResponseEntity<ApiResponse<List<ParkingSlotBooking>>> myBookings(
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.ok(sessionService.getMyBookings(principal.getId())));
    }

    @Data
    public static class CheckInRequest {
        @NotNull Long communityId;
        Long slotId;
        @NotBlank String vehicleNumber;
        ParkingSession.SessionType sessionType;
    }

    @Data
    public static class SlotBookingRequest {
        @NotNull Long communityId;
        @NotNull Long slotId;
        String guestName;
        String guestVehicle;
        @NotNull LocalDate bookingDate;
        @NotNull LocalTime startTime;
        @NotNull LocalTime endTime;
    }
}
