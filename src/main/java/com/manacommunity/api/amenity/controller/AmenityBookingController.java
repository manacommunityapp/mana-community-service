package com.manacommunity.api.amenity.controller;

import com.manacommunity.api.amenity.entity.*;
import com.manacommunity.api.amenity.repository.FacilityRepository;
import com.manacommunity.api.amenity.service.SlotBookingService;
import com.manacommunity.api.amenity.service.WaitlistPromotionService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/amenity-booking")
@RequiredArgsConstructor
public class AmenityBookingController {

    private final FacilityRepository facilityRepository;
    private final SlotBookingService bookingService;
    private final WaitlistPromotionService waitlistService;

    // ── Facilities ──

    @GetMapping("/facilities/{communityId}")
    public ResponseEntity<List<Facility>> getFacilities(@PathVariable Long communityId) {
        return ResponseEntity.ok(facilityRepository.findByCommunityIdAndActiveTrue(communityId));
    }

    @PostMapping("/facilities")
    @PreAuthorize("hasAuthority('View Admin')")
    public ResponseEntity<Facility> createFacility(@RequestBody Facility facility) {
        return ResponseEntity.ok(facilityRepository.save(facility));
    }

    // ── Slot Management ──

    @GetMapping("/slots/{facilityId}")
    public ResponseEntity<List<FacilitySlot>> getSlots(
            @PathVariable Long facilityId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(bookingService.getSlotsForDate(facilityId, date));
    }

    @GetMapping("/slots/{facilityId}/available")
    public ResponseEntity<List<FacilitySlot>> getAvailableSlots(
            @PathVariable Long facilityId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(bookingService.getAvailableSlots(facilityId, date));
    }

    @PostMapping("/slots/{facilityId}/generate")
    @PreAuthorize("hasAuthority('View Admin')")
    public ResponseEntity<List<FacilitySlot>> generateSlots(
            @PathVariable Long facilityId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(bookingService.generateSlotsForDate(facilityId, date));
    }

    // ── Booking ──

    @PostMapping("/book/{slotId}")
    public ResponseEntity<SlotBookingService.BookingResult> bookSlot(
            @PathVariable Long slotId, @RequestBody BookRequest request) {
        return ResponseEntity.ok(bookingService.bookSlot(slotId, request.residentId(), request.flatNo()));
    }

    @PostMapping("/cancel/{slotId}")
    public ResponseEntity<SlotBookingService.CancelResult> cancelBooking(
            @PathVariable Long slotId, @RequestBody Map<String, Long> body) {
        Long residentId = body.get("residentId");
        return ResponseEntity.ok(bookingService.cancelBooking(slotId, residentId));
    }

    // ── Waitlist ──

    @PostMapping("/waitlist/{slotId}/join")
    public ResponseEntity<WaitlistPromotionService.WaitlistResult> joinWaitlist(
            @PathVariable Long slotId, @RequestBody BookRequest request) {
        return ResponseEntity.ok(waitlistService.joinWaitlist(slotId, request.residentId(), request.flatNo()));
    }

    @PostMapping("/waitlist/{slotId}/accept")
    public ResponseEntity<SlotBookingService.BookingResult> acceptOffer(
            @PathVariable Long slotId, @RequestBody Map<String, Long> body) {
        Long residentId = body.get("residentId");
        return ResponseEntity.ok(waitlistService.acceptOffer(slotId, residentId));
    }

    @PostMapping("/waitlist/{slotId}/cancel")
    public ResponseEntity<Map<String, String>> cancelWaitlist(
            @PathVariable Long slotId, @RequestBody Map<String, Long> body) {
        waitlistService.cancelFromWaitlist(slotId, body.get("residentId"));
        return ResponseEntity.ok(Map.of("status", "removed"));
    }

    @GetMapping("/waitlist/{slotId}/size")
    public ResponseEntity<Map<String, Long>> waitlistSize(@PathVariable Long slotId) {
        return ResponseEntity.ok(Map.of("size", waitlistService.getWaitlistSize(slotId)));
    }

    @PostMapping("/waitlist/process-expired")
    @PreAuthorize("hasAuthority('View Admin')")
    public ResponseEntity<Map<String, Integer>> processExpired() {
        int promoted = waitlistService.processExpiredOffers();
        return ResponseEntity.ok(Map.of("promotedCount", promoted));
    }

    @GetMapping("/waitlist/my-entries/{residentId}")
    public ResponseEntity<List<BookingWaitlist>> myWaitlist(@PathVariable Long residentId) {
        return ResponseEntity.ok(waitlistService.getMyWaitlistEntries(residentId));
    }

    public record BookRequest(Long residentId, String flatNo) {}
}
