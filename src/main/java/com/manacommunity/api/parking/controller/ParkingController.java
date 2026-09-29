package com.manacommunity.api.parking.controller;

import com.manacommunity.api.parking.dto.*;
import com.manacommunity.api.parking.service.ParkingService;
import com.manacommunity.api.service.PermissionCheckService;
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

import static com.manacommunity.api.constants.PermissionConstants.MANAGE_PARKING;
import static com.manacommunity.api.constants.PermissionConstants.VIEW_PARKING;

@RestController
@RequestMapping("/api/parking")
@RequiredArgsConstructor
public class ParkingController {

    private final ParkingService parkingService;
    private final LoggedInUserService loggedInUserService;
    private final PermissionCheckService permissionCheckService;

    // ── Slots ────────────────────────────────────────────────────────

    @GetMapping("/slots")
    public ResponseEntity<List<ParkingSlotResponse>> getSlots(
            @AuthenticationPrincipal UserPrincipal principal) {
        permissionCheckService.requireAnyPermission(principal, VIEW_PARKING);
        AppUser caller = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(parkingService.getSlots(caller.getCommunity().getId()));
    }

    @GetMapping("/slots/available")
    public ResponseEntity<List<ParkingSlotResponse>> getAvailableSlots(
            @AuthenticationPrincipal UserPrincipal principal) {
        permissionCheckService.requireAnyPermission(principal, VIEW_PARKING);
        AppUser caller = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(parkingService.getAvailableSlots(caller.getCommunity().getId()));
    }

    @PostMapping("/slots")
    public ResponseEntity<ParkingSlotResponse> createSlot(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody ParkingSlotRequest request) {
        permissionCheckService.requireAnyPermission(principal, MANAGE_PARKING);
        AppUser caller = loggedInUserService.resolve(principal);
        return ResponseEntity.status(HttpStatus.CREATED).body(parkingService.createSlot(caller, request));
    }

    @PutMapping("/slots/{id}")
    public ResponseEntity<ParkingSlotResponse> updateSlot(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id,
            @Valid @RequestBody ParkingSlotRequest request) {
        permissionCheckService.requireAnyPermission(principal, MANAGE_PARKING);
        AppUser caller = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(parkingService.updateSlot(caller, id, request));
    }

    @PostMapping("/slots/{id}/assign")
    public ResponseEntity<ParkingSlotResponse> assignSlot(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id,
            @RequestParam Long userId) {
        permissionCheckService.requireAnyPermission(principal, MANAGE_PARKING);
        AppUser caller = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(parkingService.assignSlot(id, userId, caller));
    }

    @PostMapping("/slots/{id}/release")
    public ResponseEntity<ParkingSlotResponse> releaseSlot(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id) {
        permissionCheckService.requireAnyPermission(principal, MANAGE_PARKING);
        return ResponseEntity.ok(parkingService.releaseSlot(id));
    }

    @DeleteMapping("/slots/{id}")
    public ResponseEntity<Void> deleteSlot(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id) {
        permissionCheckService.requireAnyPermission(principal, MANAGE_PARKING);
        parkingService.deleteSlot(id);
        return ResponseEntity.noContent().build();
    }

    // ── Vehicles ─────────────────────────────────────────────────────

    @GetMapping("/vehicles")
    public ResponseEntity<List<ResidentVehicleResponse>> getVehicles(
            @AuthenticationPrincipal UserPrincipal principal) {
        permissionCheckService.requireAnyPermission(principal, VIEW_PARKING);
        AppUser caller = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(parkingService.getVehicles(caller.getCommunity().getId()));
    }

    @GetMapping("/vehicles/mine")
    public ResponseEntity<List<ResidentVehicleResponse>> getMyVehicles(
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser caller = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(parkingService.getMyVehicles(caller.getId()));
    }

    @PostMapping("/vehicles")
    public ResponseEntity<ResidentVehicleResponse> registerVehicle(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody ResidentVehicleRequest request) {
        AppUser caller = loggedInUserService.resolve(principal);
        return ResponseEntity.status(HttpStatus.CREATED).body(parkingService.registerVehicle(caller, request));
    }

    @PutMapping("/vehicles/{id}")
    public ResponseEntity<ResidentVehicleResponse> updateVehicle(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id,
            @Valid @RequestBody ResidentVehicleRequest request) {
        AppUser caller = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(parkingService.updateVehicle(caller, id, request));
    }

    @DeleteMapping("/vehicles/{id}")
    public ResponseEntity<Void> deleteVehicle(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id) {
        AppUser caller = loggedInUserService.resolve(principal);
        parkingService.deleteVehicle(id);
        return ResponseEntity.noContent().build();
    }
}
