package com.manacommunity.api.vehicle.controller;

import com.manacommunity.api.vehicle.dto.RegisterVehicleRequest;
import com.manacommunity.api.vehicle.entity.VehicleRegistration;
import com.manacommunity.api.vehicle.service.VehicleService;
import com.manacommunity.api.user.model.AppUser;
import com.manacommunity.api.user.security.UserPrincipal;
import com.manacommunity.api.user.service.LoggedInUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/vehicles")
@RequiredArgsConstructor
public class VehicleController {

    private final VehicleService vehicleService;
    private final LoggedInUserService loggedInUserService;

    @GetMapping("/my-vehicles")
    public ResponseEntity<List<VehicleRegistration>> getMyVehicles(@AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(vehicleService.getMyVehicles(user));
    }

    @PostMapping("/register")
    public ResponseEntity<VehicleRegistration> register(@RequestBody RegisterVehicleRequest req,
                                                         @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.status(HttpStatus.CREATED).body(vehicleService.register(req, user));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> remove(@PathVariable Long id, @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        vehicleService.remove(id, user);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/whitelist")
    public ResponseEntity<VehicleRegistration> toggleWhitelist(@PathVariable Long id,
                                                                @RequestBody Map<String, Boolean> body,
                                                                @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        boolean enabled = Boolean.TRUE.equals(body.get("enabled"));
        return ResponseEntity.ok(vehicleService.toggleWhitelist(id, enabled, user));
    }
}
