package com.manacommunity.api.vehicle.controller;

import com.manacommunity.api.user.model.AppUser;
import com.manacommunity.api.user.security.UserPrincipal;
import com.manacommunity.api.user.service.LoggedInUserService;
import com.manacommunity.api.vehicle.entity.VehicleBlacklist;
import com.manacommunity.api.vehicle.service.VehicleBlacklistService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/vehicles/blacklist")
@RequiredArgsConstructor
public class VehicleBlacklistController {

    private final VehicleBlacklistService vehicleBlacklistService;
    private final LoggedInUserService loggedInUserService;

    @GetMapping
    public ResponseEntity<List<VehicleBlacklist>> getBlacklist(
            @RequestParam Long communityId) {
        return ResponseEntity.ok(vehicleBlacklistService.getBlacklist(communityId));
    }

    @PostMapping
    public ResponseEntity<VehicleBlacklist> addToBlacklist(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestBody AddBlacklistRequest request) {
        AppUser caller = loggedInUserService.resolve(principal);
        VehicleBlacklist entry = vehicleBlacklistService.addToBlacklist(
                request.getCommunityId(), request.getPlateNumber(), request.getReason(), caller.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(entry);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> removeFromBlacklist(@PathVariable Long id) {
        vehicleBlacklistService.removeFromBlacklist(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/check")
    public ResponseEntity<java.util.Map<String, Boolean>> checkBlacklist(
            @RequestParam Long communityId,
            @RequestParam String plateNumber) {
        boolean blacklisted = vehicleBlacklistService.isBlacklisted(communityId, plateNumber);
        return ResponseEntity.ok(java.util.Map.of("blacklisted", blacklisted));
    }

    @Data
    public static class AddBlacklistRequest {
        private Long communityId;
        private String plateNumber;
        private String reason;
    }
}
