package com.manacommunity.api.vehicle.controller;

import com.manacommunity.api.user.model.AppUser;
import com.manacommunity.api.user.security.UserPrincipal;
import com.manacommunity.api.user.service.LoggedInUserService;
import com.manacommunity.api.vehicle.entity.VisitorVehiclePass;
import com.manacommunity.api.vehicle.repository.VisitorVehiclePassRepository;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/vehicles/visitor-passes")
@RequiredArgsConstructor
public class VisitorVehiclePassController {

    private final VisitorVehiclePassRepository repo;
    private final LoggedInUserService loggedInUserService;

    @PostMapping
    public ResponseEntity<VisitorVehiclePass> createPass(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestBody CreatePassRequest request) {
        AppUser caller = loggedInUserService.resolve(principal);
        VisitorVehiclePass pass = repo.save(VisitorVehiclePass.builder()
                .communityId(caller.getCommunity().getId())
                .residentUserId(caller.getId())
                .plateNumber(request.getPlateNumber().toUpperCase().trim())
                .visitorName(request.getVisitorName())
                .validDate(request.getValidDate())
                .build());
        return ResponseEntity.status(HttpStatus.CREATED).body(pass);
    }

    @GetMapping
    public ResponseEntity<List<VisitorVehiclePass>> getPasses(
            @RequestParam Long communityId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        LocalDate effectiveDate = date != null ? date : LocalDate.now();
        return ResponseEntity.ok(
                repo.findByCommunityIdAndValidDateAndStatusOrderByCreatedAtDesc(communityId, effectiveDate, "ACTIVE"));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> revokePass(@PathVariable Long id) {
        repo.findById(id).ifPresent(p -> {
            p.setStatus("REVOKED");
            repo.save(p);
        });
        return ResponseEntity.noContent().build();
    }

    @Data
    public static class CreatePassRequest {
        private String plateNumber;
        private String visitorName;
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        private LocalDate validDate;
    }
}
