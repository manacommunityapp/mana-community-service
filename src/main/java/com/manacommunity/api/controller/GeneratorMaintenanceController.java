package com.manacommunity.api.controller;

import com.manacommunity.api.dto.GeneratorDtos.*;
import com.manacommunity.api.model.*;
import com.manacommunity.api.service.GeneratorMaintenanceService;
import com.manacommunity.api.user.model.AppUser;
import com.manacommunity.api.user.security.UserPrincipal;
import com.manacommunity.api.user.service.LoggedInUserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/generator-maintenance")
@RequiredArgsConstructor
public class GeneratorMaintenanceController {

    private final GeneratorMaintenanceService generatorMaintenanceService;
    private final LoggedInUserService loggedInUserService;

    @GetMapping("/generators")
    public ResponseEntity<List<GeneratorAsset>> getGenerators(@AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(generatorMaintenanceService.getGenerators(user.getCommunity().getId()));
    }

    @GetMapping("/generators/{id}")
    public ResponseEntity<GeneratorAsset> getGenerator(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(generatorMaintenanceService.getGenerator(user.getCommunity().getId(), id));
    }

    @PostMapping("/runs")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN','GUARD','STAFF')")
    public ResponseEntity<GeneratorRunLog> logRun(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody GeneratorRunRequest request) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(generatorMaintenanceService.logRun(user.getCommunity().getId(), request));
    }

    @GetMapping("/runs")
    public ResponseEntity<List<GeneratorRunLog>> getRunLogs(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false) Long generatorId) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(generatorMaintenanceService.getRunLogs(user.getCommunity().getId(), generatorId));
    }

    @PostMapping("/refills")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN','GUARD','STAFF')")
    public ResponseEntity<GeneratorFuelRefill> logRefill(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody GeneratorRefillRequest request) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(generatorMaintenanceService.logRefill(user.getCommunity().getId(), request));
    }

    @GetMapping("/refills")
    public ResponseEntity<List<GeneratorFuelRefill>> getFuelRefills(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false) Long generatorId) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(generatorMaintenanceService.getFuelRefills(user.getCommunity().getId(), generatorId));
    }

    @PostMapping("/services")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN','STAFF')")
    public ResponseEntity<GeneratorServiceRecord> recordService(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody GeneratorServiceRecordRequest request) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(generatorMaintenanceService.recordService(user.getCommunity().getId(), request));
    }

    @GetMapping("/services")
    public ResponseEntity<List<GeneratorServiceRecord>> getServiceRecords(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false) Long generatorId) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(generatorMaintenanceService.getServiceRecords(user.getCommunity().getId(), generatorId));
    }

    @GetMapping("/amc")
    public ResponseEntity<List<GeneratorAmcContract>> getAmcContracts(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false) Long generatorId) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(generatorMaintenanceService.getAmcContracts(user.getCommunity().getId(), generatorId));
    }

    @GetMapping("/alerts")
    public ResponseEntity<List<ServiceAlertDto>> checkServiceAlerts(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false) Long generatorId) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(generatorMaintenanceService.checkServiceAlerts(user.getCommunity().getId(), generatorId));
    }
}
