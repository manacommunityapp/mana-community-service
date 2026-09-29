package com.manacommunity.api.parking.controller;

import com.manacommunity.api.parking.dto.ParkingSpotDto;
import com.manacommunity.api.parking.dto.ReserveSpotRequest;
import com.manacommunity.api.parking.dto.VisitorPassDto;
import com.manacommunity.api.parking.dto.VisitorPassRequest;
import com.manacommunity.api.parking.service.ParkingService;
import com.manacommunity.api.user.model.AppUser;
import com.manacommunity.api.user.security.UserPrincipal;
import com.manacommunity.api.user.service.LoggedInUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping({"/api/parking", "/parking"})
@RequiredArgsConstructor
@Tag(name = "Parking Management", description = "Endpoints for managing parking spots, reservations, and visitor passes")
public class ParkingController {

    private final LoggedInUserService loggedInUserService;
    private final ParkingService parkingService;

    @GetMapping("/spots")
    @Operation(summary = "List parking spots with availability", description = "Returns parking spots with optional filters for type, status, and level")
    public ResponseEntity<List<ParkingSpotDto>> getSpots(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String level) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(parkingService.getSpots(user, type, status, level));
    }

    @PostMapping("/reserve")
    @Operation(summary = "Reserve or assign a parking spot", description = "Reserves an available parking spot for the authenticated resident")
    public ResponseEntity<ParkingSpotDto> reserveSpot(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody ReserveSpotRequest request) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(parkingService.reserveSpot(user, request));
    }

    @GetMapping("/my-spots")
    @Operation(summary = "List current user's assigned spots", description = "Returns spots currently assigned or reserved by the authenticated resident")
    public ResponseEntity<List<ParkingSpotDto>> getMySpots(
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(parkingService.getMySpots(user));
    }

    @PostMapping("/visitor-pass")
    @Operation(summary = "Issue a temporary visitor parking pass", description = "Generates a temporary parking pass with a unique pass code for a guest vehicle")
    public ResponseEntity<VisitorPassDto> createVisitorPass(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody VisitorPassRequest request) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.status(HttpStatus.CREATED).body(parkingService.createVisitorPass(user, request));
    }
}
