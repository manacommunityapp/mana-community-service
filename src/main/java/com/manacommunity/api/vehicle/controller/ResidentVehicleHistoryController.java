package com.manacommunity.api.vehicle.controller;

import com.manacommunity.api.safety.model.AnprEvent;
import com.manacommunity.api.safety.repository.AnprEventRepository;
import com.manacommunity.api.user.model.AppUser;
import com.manacommunity.api.user.security.UserPrincipal;
import com.manacommunity.api.user.service.LoggedInUserService;
import com.manacommunity.api.vehicle.entity.VehicleRegistration;
import com.manacommunity.api.vehicle.repository.VehicleRegistrationRepository;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/resident/vehicles")
@RequiredArgsConstructor
public class ResidentVehicleHistoryController {

    private final VehicleRegistrationRepository vehicleRegistrationRepository;
    private final AnprEventRepository anprEventRepository;
    private final LoggedInUserService loggedInUserService;

    @GetMapping("/history")
    public ResponseEntity<List<VehicleHistoryEntry>> getVehicleHistory(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        AppUser caller = loggedInUserService.resolve(principal);
        Long communityId = caller.getCommunity().getId();

        Set<String> plates = vehicleRegistrationRepository
                .findByOwnerIdAndStatus(caller.getId(), "ACTIVE")
                .stream()
                .map(VehicleRegistration::getPlateNumber)
                .collect(Collectors.toSet());

        if (plates.isEmpty()) {
            return ResponseEntity.ok(List.of());
        }

        List<AnprEvent> events = anprEventRepository.findByCommunityIdOrderByTimestampDesc(communityId);
        List<VehicleHistoryEntry> history = events.stream()
                .filter(e -> plates.contains(e.getPlateNumber()))
                .skip((long) page * size)
                .limit(size)
                .map(e -> VehicleHistoryEntry.builder()
                        .plateNumber(e.getPlateNumber())
                        .direction(e.getDirection().name())
                        .timestamp(e.getTimestamp())
                        .gateName(e.getGate())
                        .build())
                .collect(Collectors.toList());

        return ResponseEntity.ok(history);
    }

    @Data
    @Builder
    public static class VehicleHistoryEntry {
        private String plateNumber;
        private String direction;
        private LocalDateTime timestamp;
        private String gateName;
    }
}
