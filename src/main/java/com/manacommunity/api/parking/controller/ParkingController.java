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
}
