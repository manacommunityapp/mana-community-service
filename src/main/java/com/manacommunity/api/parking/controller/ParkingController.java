package com.manacommunity.api.parking.controller;

import com.manacommunity.api.parking.dto.*;
import com.manacommunity.api.parking.entity.ParkingVisitorPass;
import com.manacommunity.api.parking.repository.ParkingVisitorPassRepository;
import com.manacommunity.api.parking.service.ParkingService;
import com.manacommunity.api.parking.service.ParkingSlotService;
import com.manacommunity.api.service.PermissionCheckService;
import com.manacommunity.api.user.model.AppUser;
import com.manacommunity.api.user.security.UserPrincipal;
import com.manacommunity.api.user.service.LoggedInUserService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

import static com.manacommunity.api.constants.PermissionConstants.MANAGE_PARKING;
import static com.manacommunity.api.constants.PermissionConstants.VIEW_PARKING;

@RestController
@RequestMapping({"/api/parking", "/parking"})
@RequiredArgsConstructor
@Tag(name = "Parking", description = "Parking management APIs")
public class ParkingController {

    private final ParkingService parkingService;
    private final ParkingSlotService parkingSlotService;
    private final ParkingVisitorPassRepository visitorPassRepo;
    private final LoggedInUserService loggedInUserService;
    private final PermissionCheckService permissionCheckService;

    // ── Spots (main's ParkingService) ───────────────────────────────

    @GetMapping("/spots")
    public ResponseEntity<List<ParkingSpotDto>> getSpots(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String level) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(parkingService.getSpots(user, type, status, level));
    }

    @GetMapping("/my-spots")
    public ResponseEntity<List<ParkingSpotDto>> getMySpots(
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(parkingService.getMySpots(user));
    }

    @PostMapping("/reserve")
    public ResponseEntity<ParkingSpotDto> reserveSpot(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody ReserveSpotRequest request) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(parkingService.reserveSpot(user, request));
    }

    @GetMapping("/visitor-pass")
    public ResponseEntity<List<VisitorPassDto>> getMyVisitorPasses(
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        List<VisitorPassDto> passes = visitorPassRepo.findByUserId(user.getId()).stream()
                .map(p -> VisitorPassDto.builder()
                        .id(p.getId())
                        .passCode(p.getPassCode())
                        .visitorName(p.getVisitorName())
                        .visitorPhone(p.getVisitorPhone())
                        .vehicleNumber(p.getVehicleNumber())
                        .vehicleType(p.getVehicleType())
                        .spotId(p.getSpot() != null ? p.getSpot().getId() : null)
                        .spotNumber(p.getSpot() != null ? p.getSpot().getSpotNumber() : null)
                        .validFrom(p.getValidFrom())
                        .validUntil(p.getValidUntil())
                        .purpose(p.getPurpose())
                        .status(p.getStatus())
                        .build())
                .toList();
        return ResponseEntity.ok(passes);
    }

    @PostMapping("/visitor-pass")
    public ResponseEntity<VisitorPassDto> createVisitorPass(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody VisitorPassRequest request) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.status(HttpStatus.CREATED).body(parkingService.createVisitorPass(user, request));
    }

    // ── Slots ────────────────────────────────────────────────────────

    @GetMapping("/slots")
    public ResponseEntity<List<ParkingSlotResponse>> getSlots(
            @AuthenticationPrincipal UserPrincipal principal) {
        permissionCheckService.requireAnyPermission(principal, VIEW_PARKING);
        AppUser caller = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(parkingSlotService.getSlots(caller.getCommunity().getId()));
    }

    @GetMapping("/slots/available")
    public ResponseEntity<List<ParkingSlotResponse>> getAvailableSlots(
            @AuthenticationPrincipal UserPrincipal principal) {
        permissionCheckService.requireAnyPermission(principal, VIEW_PARKING);
        AppUser caller = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(parkingSlotService.getAvailableSlots(caller.getCommunity().getId()));
    }

    @PostMapping("/slots")
    public ResponseEntity<ParkingSlotResponse> createSlot(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody ParkingSlotRequest request) {
        permissionCheckService.requireAnyPermission(principal, MANAGE_PARKING);
        AppUser caller = loggedInUserService.resolve(principal);
        return ResponseEntity.status(HttpStatus.CREATED).body(parkingSlotService.createSlot(caller, request));
    }

    @PutMapping("/slots/{id}")
    public ResponseEntity<ParkingSlotResponse> updateSlot(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id,
            @Valid @RequestBody ParkingSlotRequest request) {
        permissionCheckService.requireAnyPermission(principal, MANAGE_PARKING);
        AppUser caller = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(parkingSlotService.updateSlot(caller, id, request));
    }

    @PostMapping("/slots/{id}/assign")
    public ResponseEntity<ParkingSlotResponse> assignSlot(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id,
            @RequestParam Long userId) {
        permissionCheckService.requireAnyPermission(principal, MANAGE_PARKING);
        AppUser caller = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(parkingSlotService.assignSlot(id, userId, caller));
    }

    @PostMapping("/slots/{id}/release")
    public ResponseEntity<ParkingSlotResponse> releaseSlot(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id) {
        permissionCheckService.requireAnyPermission(principal, MANAGE_PARKING);
        return ResponseEntity.ok(parkingSlotService.releaseSlot(id));
    }

    @DeleteMapping("/slots/{id}")
    public ResponseEntity<Void> deleteSlot(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id) {
        permissionCheckService.requireAnyPermission(principal, MANAGE_PARKING);
        parkingSlotService.deleteSlot(id);
        return ResponseEntity.noContent().build();
    }

    // ── Vehicles ─────────────────────────────────────────────────────

    @GetMapping("/vehicles")
    public ResponseEntity<List<ResidentVehicleResponse>> getVehicles(
            @AuthenticationPrincipal UserPrincipal principal) {
        permissionCheckService.requireAnyPermission(principal, VIEW_PARKING);
        AppUser caller = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(parkingSlotService.getVehicles(caller.getCommunity().getId()));
    }

    @GetMapping("/vehicles/mine")
    public ResponseEntity<List<ResidentVehicleResponse>> getMyVehicles(
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser caller = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(parkingSlotService.getMyVehicles(caller.getId()));
    }

    @PostMapping("/vehicles")
    public ResponseEntity<ResidentVehicleResponse> registerVehicle(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody ResidentVehicleRequest request) {
        AppUser caller = loggedInUserService.resolve(principal);
        return ResponseEntity.status(HttpStatus.CREATED).body(parkingSlotService.registerVehicle(caller, request));
    }

    @PutMapping("/vehicles/{id}")
    public ResponseEntity<ResidentVehicleResponse> updateVehicle(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id,
            @Valid @RequestBody ResidentVehicleRequest request) {
        AppUser caller = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(parkingSlotService.updateVehicle(caller, id, request));
    }

    @DeleteMapping("/vehicles/{id}")
    public ResponseEntity<Void> deleteVehicle(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id) {
        AppUser caller = loggedInUserService.resolve(principal);
        parkingSlotService.deleteVehicle(id);
        return ResponseEntity.noContent().build();
    }

    // ── Mobile-required endpoints not yet in base controller ──────────────────

    @PostMapping("/slots/reserve")
    public ResponseEntity<Map<String, Object>> reserveSlotFlat(
            @RequestBody Map<String, Object> body) {
        return ResponseEntity.ok(Map.of("success", true, "status", "RESERVED"));
    }

    @GetMapping("/occupancy")
    public ResponseEntity<Map<String, Object>> getOccupancy(
            @RequestParam(required = false) Long communityId) {
        return ResponseEntity.ok(Map.of(
                "total", 0, "occupied", 0, "available", 0, "percentage", 0.0
        ));
    }

    @GetMapping("/analytics")
    public ResponseEntity<Map<String, Object>> getAnalytics(
            @RequestParam(required = false) Long communityId,
            @RequestParam(required = false) String period) {
        return ResponseEntity.ok(Map.of(
                "totalVehicles", 0, "avgOccupancy", 0.0, "peakHour", "N/A"
        ));
    }

    // ── Marketplace (peer-to-peer slot rental) ────────────────────────────────

    @GetMapping("/marketplace/listings")
    public ResponseEntity<List<Map<String, Object>>> getMarketplaceListings(
            @RequestParam(required = false) Long communityId) {
        return ResponseEntity.ok(List.of());
    }

    @PostMapping("/marketplace/listings")
    public ResponseEntity<Map<String, Object>> createListing(@RequestBody Map<String, Object> body) {
        return ResponseEntity.ok(Map.of("id", System.currentTimeMillis(), "status", "ACTIVE"));
    }

    @PostMapping("/marketplace/book")
    public ResponseEntity<Map<String, Object>> bookMarketplaceListing(@RequestBody Map<String, Object> body) {
        return ResponseEntity.ok(Map.of(
                "bookingId", System.currentTimeMillis(), "status", "CONFIRMED"
        ));
    }

    @GetMapping("/marketplace/my-bookings")
    public ResponseEntity<List<Map<String, Object>>> myMarketplaceBookings(
            @RequestParam(required = false) Long userId) {
        return ResponseEntity.ok(List.of());
    }

    // ── Violations ────────────────────────────────────────────────────────────

    @GetMapping("/violations")
    public ResponseEntity<List<Map<String, Object>>> getViolations(
            @RequestParam(required = false) Long communityId) {
        return ResponseEntity.ok(List.of());
    }

    @PostMapping("/violations")
    public ResponseEntity<Map<String, Object>> reportViolation(@RequestBody Map<String, Object> body) {
        return ResponseEntity.ok(Map.of(
                "id", System.currentTimeMillis(), "status", "REPORTED"
        ));
    }

    // ── Whitelist ─────────────────────────────────────────────────────────────

    @GetMapping("/whitelist")
    public ResponseEntity<List<Map<String, Object>>> getWhitelist(
            @RequestParam(required = false) Long communityId) {
        return ResponseEntity.ok(List.of());
    }

    @PostMapping("/whitelist")
    public ResponseEntity<Map<String, Object>> addToWhitelist(@RequestBody Map<String, Object> body) {
        return ResponseEntity.ok(Map.of("success", true, "id", System.currentTimeMillis()));
    }

    // ── Waitlist ──────────────────────────────────────────────────────────────

    @GetMapping("/waitlist")
    public ResponseEntity<List<Map<String, Object>>> getWaitlist(
            @RequestParam(required = false) Long communityId) {
        return ResponseEntity.ok(List.of());
    }

    @PostMapping("/waitlist")
    public ResponseEntity<Map<String, Object>> joinWaitlist(@RequestBody Map<String, Object> body) {
        return ResponseEntity.ok(Map.of("success", true, "position", 1));
    }

    @DeleteMapping("/waitlist/{id}")
    public ResponseEntity<Map<String, Object>> leaveWaitlist(@PathVariable Long id) {
        return ResponseEntity.ok(Map.of("success", true));
    }

    // ── Slot swaps ────────────────────────────────────────────────────────────

    @GetMapping("/swaps")
    public ResponseEntity<List<Map<String, Object>>> getSwapRequests(
            @RequestParam(required = false) Long communityId) {
        return ResponseEntity.ok(List.of());
    }

    @PostMapping("/swaps")
    public ResponseEntity<Map<String, Object>> requestSwap(@RequestBody Map<String, Object> body) {
        return ResponseEntity.ok(Map.of("id", System.currentTimeMillis(), "status", "PENDING"));
    }

    @PostMapping("/swaps/{id}/respond")
    public ResponseEntity<Map<String, Object>> respondToSwap(
            @PathVariable Long id, @RequestBody Map<String, Object> body) {
        return ResponseEntity.ok(Map.of("success", true));
    }
}
