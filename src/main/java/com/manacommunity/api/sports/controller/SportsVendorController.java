package com.manacommunity.api.sports.controller;

import com.manacommunity.api.sports.dto.SportsVendorDtos.*;
import com.manacommunity.api.sports.service.SportsVendorService;
import com.manacommunity.api.user.model.AppUser;
import com.manacommunity.api.user.security.UserPrincipal;
import com.manacommunity.api.user.service.LoggedInUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * REST Controller for Mana Community Sports Vendor Portal & Court Marketplace.
 */
@RestController
@RequestMapping("/api/sports/vendor")
@RequiredArgsConstructor
public class SportsVendorController {

    private final SportsVendorService vendorService;
    private final LoggedInUserService loggedInUserService;

    // ── 1. Facilities / Arenas ─────────────────────────────────────────────────

    @GetMapping("/facilities")
    public ResponseEntity<List<FacilityResponse>> getFacilities(
            @RequestParam(required = false) String sport,
            @RequestParam(required = false) String city,
            @RequestParam(required = false) Long vendorId) {
        return ResponseEntity.ok(vendorService.getFacilities(sport, city, vendorId));
    }

    @GetMapping("/facilities/{id}")
    public ResponseEntity<FacilityResponse> getFacilityById(@PathVariable Long id) {
        return ResponseEntity.ok(vendorService.getFacilityById(id));
    }

    // ── 2. Courts & Slot Inventory ─────────────────────────────────────────────

    @GetMapping("/facilities/{facilityId}/courts")
    public ResponseEntity<List<CourtResponse>> getCourts(@PathVariable Long facilityId) {
        return ResponseEntity.ok(vendorService.getCourts(facilityId));
    }

    @GetMapping("/courts/{courtId}/slots")
    public ResponseEntity<List<SlotResponse>> getSlots(
            @PathVariable Long courtId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        LocalDate targetDate = (date != null) ? date : LocalDate.now();
        return ResponseEntity.ok(vendorService.getSlots(courtId, targetDate));
    }

    @PostMapping("/courts/{courtId}/slots/toggle-status")
    public ResponseEntity<SlotResponse> toggleSlotStatus(
            @PathVariable Long courtId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam String startTime,
            @RequestParam(required = false, defaultValue = "MAINTENANCE") String targetStatus) {
        return ResponseEntity.ok(vendorService.toggleSlotStatus(courtId, date, startTime, targetStatus));
    }

    // ── 3. Court Reservations & QR Check-In ────────────────────────────────────

    @PostMapping("/bookings/reserve")
    public ResponseEntity<BookingResponse> reserveSlot(
            @RequestBody CreateBookingRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        Long residentId = 1L;
        if (principal != null) {
            AppUser user = loggedInUserService.resolve(principal);
            if (user != null && user.getId() != null) {
                residentId = user.getId();
            }
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(vendorService.reserveSlot(residentId, request));
    }

    @PostMapping("/bookings/{bookingId}/check-in")
    public ResponseEntity<BookingResponse> checkInResident(
            @PathVariable Long bookingId,
            @RequestParam(required = false) String qrToken,
            @AuthenticationPrincipal UserPrincipal principal) {
        Long staffUserId = 1L;
        if (principal != null) {
            AppUser user = loggedInUserService.resolve(principal);
            if (user != null && user.getId() != null) {
                staffUserId = user.getId();
            }
        }
        return ResponseEntity.ok(vendorService.checkInResident(bookingId, qrToken, staffUserId));
    }

    @GetMapping("/bookings")
    public ResponseEntity<List<BookingResponse>> getBookings(
            @RequestParam(required = false) Long venueId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(vendorService.getBookings(venueId, date));
    }

    // ── 4. Coaching & Batches ──────────────────────────────────────────────────

    @GetMapping("/coaching/coaches")
    public ResponseEntity<List<CoachResponse>> getCoaches(@RequestParam(required = false) Long vendorId) {
        return ResponseEntity.ok(vendorService.getCoaches(vendorId));
    }

    @GetMapping("/coaching/batches")
    public ResponseEntity<List<BatchResponse>> getBatches(@RequestParam(required = false) Long venueId) {
        return ResponseEntity.ok(vendorService.getBatches(venueId));
    }

    // ── 5. Match Officials ─────────────────────────────────────────────────────

    @GetMapping("/officials")
    public ResponseEntity<List<OfficialResponse>> getOfficials(@RequestParam(required = false) String sport) {
        return ResponseEntity.ok(vendorService.getOfficials(sport));
    }

    // ── 6. Vendor Dashboard Analytics ──────────────────────────────────────────

    @GetMapping("/dashboard/stats")
    public ResponseEntity<Map<String, Object>> getDashboardStats(@RequestParam(required = false) Long vendorId) {
        return ResponseEntity.ok(vendorService.getDashboardStats(vendorId));
    }
}
