package com.manacommunity.api.mobile;

import com.manacommunity.api.exception.ResourceNotFoundException;
import com.manacommunity.api.exception.UnauthorizedActionException;
import com.manacommunity.api.model.Community;
import com.manacommunity.api.parking.entity.ResidentVehicle;
import com.manacommunity.api.parking.repository.ResidentVehicleRepository;
import com.manacommunity.api.user.model.AppUser;
import com.manacommunity.api.user.security.UserPrincipal;
import com.manacommunity.api.user.service.LoggedInUserService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Mobile-facing vehicle registration API.
 * Wraps the ResidentVehicle entity with the shape the mobile app expects.
 */
@RestController
@RequestMapping("/api/vehicles")
@PreAuthorize("isAuthenticated()")
@RequiredArgsConstructor
public class VehicleController {

    private final ResidentVehicleRepository vehicleRepo;
    private final LoggedInUserService loggedInUserService;

    @GetMapping("/my-vehicles")
    public ResponseEntity<List<Map<String, Object>>> getMyVehicles(
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        List<Map<String, Object>> vehicles = vehicleRepo.findByOwnerIdOrderByCreatedAtDesc(user.getId())
                .stream().map(this::toResponse).collect(Collectors.toList());
        return ResponseEntity.ok(vehicles);
    }

    @PostMapping("/register")
    public ResponseEntity<Map<String, Object>> registerVehicle(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody RegisterVehicleRequest req) {
        AppUser user = loggedInUserService.resolve(principal);
        Community community = user.getCommunity();
        if (community == null) {
            return ResponseEntity.badRequest().build();
        }

        ResidentVehicle.VehicleType type;
        try {
            type = ResidentVehicle.VehicleType.valueOf(req.vehicleType().toUpperCase());
        } catch (IllegalArgumentException e) {
            type = ResidentVehicle.VehicleType.OTHER;
        }

        ResidentVehicle vehicle = ResidentVehicle.builder()
                .community(community)
                .owner(user)
                .vehicleType(type)
                .make(req.make())
                .model(req.model())
                .color(req.color())
                .numberPlate(req.plateNumber().toUpperCase().trim())
                .status(ResidentVehicle.VehicleStatus.ACTIVE)
                .gateWhitelisted(false)
                .primary(vehicleRepo.countByOwnerId(user.getId()) == 0)
                .build();

        vehicle = vehicleRepo.save(vehicle);
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(vehicle));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> removeVehicle(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id) {
        AppUser user = loggedInUserService.resolve(principal);
        ResidentVehicle vehicle = vehicleRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle", id));
        if (!vehicle.getOwner().getId().equals(user.getId())) {
            throw new UnauthorizedActionException("You do not own this vehicle.");
        }
        vehicleRepo.delete(vehicle);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/whitelist")
    public ResponseEntity<Map<String, Object>> toggleGateWhitelist(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id,
            @RequestBody Map<String, Boolean> body) {
        AppUser user = loggedInUserService.resolve(principal);
        ResidentVehicle vehicle = vehicleRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle", id));
        if (!vehicle.getOwner().getId().equals(user.getId())) {
            throw new UnauthorizedActionException("You do not own this vehicle.");
        }
        boolean enabled = Boolean.TRUE.equals(body.get("enabled"));
        vehicle.setGateWhitelisted(enabled);
        vehicle = vehicleRepo.save(vehicle);
        return ResponseEntity.ok(toResponse(vehicle));
    }

    private Map<String, Object> toResponse(ResidentVehicle v) {
        String flatNumber = null;
        if (v.getOwner() != null) {
            String block = v.getOwner().getBlock();
            String flat = v.getOwner().getFlatNo();
            if (block != null && flat != null) flatNumber = block + "-" + flat;
            else if (flat != null) flatNumber = flat;
        }
        return Map.of(
                "id",               v.getId(),
                "plateNumber",      v.getNumberPlate(),
                "vehicleType",      v.getVehicleType() != null ? v.getVehicleType().name() : "OTHER",
                "make",             v.getMake() != null ? v.getMake() : "",
                "model",            v.getModel() != null ? v.getModel() : "",
                "color",            v.getColor() != null ? v.getColor() : "",
                "isGateWhitelisted",v.isGateWhitelisted(),
                "status",           v.getStatus() != null ? v.getStatus().name() : "ACTIVE",
                "registeredAt",     v.getCreatedAt() != null ? v.getCreatedAt().toString() : LocalDateTime.now().toString(),
                "flatNumber",       flatNumber != null ? flatNumber : ""
        );
    }

    public record RegisterVehicleRequest(
            @NotBlank @Size(max = 20) String plateNumber,
            @NotBlank String vehicleType,
            String make,
            String model,
            String color
    ) {}
}
