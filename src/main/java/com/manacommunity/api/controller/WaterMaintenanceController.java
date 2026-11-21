package com.manacommunity.api.controller;

import com.manacommunity.api.dto.WaterDtos.*;
import com.manacommunity.api.model.*;
import com.manacommunity.api.service.WaterMaintenanceService;
import com.manacommunity.api.user.model.AppUser;
import com.manacommunity.api.user.security.UserPrincipal;
import com.manacommunity.api.user.service.LoggedInUserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/v1/water-maintenance")
@RequiredArgsConstructor
public class WaterMaintenanceController {

    private final WaterMaintenanceService waterMaintenanceService;
    private final LoggedInUserService loggedInUserService;

    @GetMapping("/overview")
    public ResponseEntity<WaterOverviewSummaryDto> getOverviewSummary(@AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(waterMaintenanceService.getOverviewSummary(user.getCommunity().getId()));
    }

    // ── Storage Tanks ──────────────────────────────────────────────────────

    @GetMapping("/tanks")
    public ResponseEntity<List<WaterStorageTank>> getStorageTanks(@AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(waterMaintenanceService.getStorageTanks(user.getCommunity().getId()));
    }

    @GetMapping("/tanks/{id}")
    public ResponseEntity<WaterStorageTank> getStorageTank(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(waterMaintenanceService.getStorageTank(user.getCommunity().getId(), id));
    }

    @PutMapping("/tanks/{id}/level")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN','GUARD','STAFF')")
    public ResponseEntity<WaterStorageTank> updateTankLevel(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id,
            @RequestParam BigDecimal levelLiters) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(waterMaintenanceService.updateTankLevel(user.getCommunity().getId(), id, levelLiters));
    }

    // ── Water Tanker Inwarding ──────────────────────────────────────────────

    @GetMapping("/tankers")
    public ResponseEntity<List<WaterTankerDelivery>> getTankers(@AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(waterMaintenanceService.getTankerDeliveries(user.getCommunity().getId()));
    }

    @PostMapping("/tankers")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN','GUARD','STAFF')")
    public ResponseEntity<WaterTankerDelivery> logTankerDelivery(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody TankerDeliveryRequest request) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(waterMaintenanceService.logTankerDelivery(user.getCommunity().getId(), request));
    }

    // ── Pump House & HYPN Motors ───────────────────────────────────────────

    @GetMapping("/pumps")
    public ResponseEntity<List<WaterPump>> getPumps(@AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(waterMaintenanceService.getPumps(user.getCommunity().getId()));
    }

    @PostMapping("/pumps/{id}/toggle-duty")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN','STAFF')")
    public ResponseEntity<WaterPump> togglePumpDuty(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(waterMaintenanceService.togglePumpDuty(user.getCommunity().getId(), id));
    }

    @PostMapping("/pumps/runs")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN','STAFF')")
    public ResponseEntity<WaterPumpRunLog> logPumpRun(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody WaterPumpRunRequest request) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(waterMaintenanceService.logPumpRun(user.getCommunity().getId(), request));
    }

    @GetMapping("/pumps/{id}/runs")
    public ResponseEntity<List<WaterPumpRunLog>> getPumpRunLogs(@PathVariable Long id) {
        return ResponseEntity.ok(waterMaintenanceService.getPumpRunLogs(id));
    }

    // ── WTP & STP Treatment ────────────────────────────────────────────────

    @GetMapping("/treatment")
    public ResponseEntity<List<WaterTreatmentLog>> getTreatmentLogs(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false) String plantType) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(waterMaintenanceService.getTreatmentLogs(user.getCommunity().getId(), plantType));
    }

    @PostMapping("/treatment")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN','STAFF')")
    public ResponseEntity<WaterTreatmentLog> logTreatment(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody WaterTreatmentLogRequest request) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(waterMaintenanceService.logTreatment(user.getCommunity().getId(), request));
    }

    // ── Quality Testing ────────────────────────────────────────────────────

    @GetMapping("/quality-tests")
    public ResponseEntity<List<WaterQualityTest>> getQualityTests(@AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(waterMaintenanceService.getQualityTests(user.getCommunity().getId()));
    }

    @PostMapping("/quality-tests")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN','STAFF')")
    public ResponseEntity<WaterQualityTest> logQualityTest(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody WaterQualityTestRequest request) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(waterMaintenanceService.logQualityTest(user.getCommunity().getId(), request));
    }

    // ── Tank Cleaning ──────────────────────────────────────────────────────

    @GetMapping("/tanks/{tankId}/cleaning")
    public ResponseEntity<List<TankCleaningSchedule>> getCleaningSchedules(@PathVariable Long tankId) {
        return ResponseEntity.ok(waterMaintenanceService.getCleaningSchedules(tankId));
    }

    @PostMapping("/tanks/{tankId}/cleaning")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN','STAFF')")
    public ResponseEntity<TankCleaningSchedule> recordCleaning(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long tankId,
            @Valid @RequestBody TankCleaningRequest request) {
        request.setTankId(tankId);
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(waterMaintenanceService.recordCleaning(user.getCommunity().getId(), request));
    }
}
