package com.manacommunity.api.controller;

import com.manacommunity.api.dto.MeterReadingRequest;
import com.manacommunity.api.dto.SmartMeterRequest;
import com.manacommunity.api.model.MeterReading;
import com.manacommunity.api.model.SmartMeter;
import com.manacommunity.api.service.SmartMeterService;
import com.manacommunity.api.user.model.AppUser;
import com.manacommunity.api.user.security.UserPrincipal;
import com.manacommunity.api.user.service.LoggedInUserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/iot")
@RequiredArgsConstructor
public class SmartMeterController {

    private final SmartMeterService smartMeterService;
    private final LoggedInUserService loggedInUserService;

    @GetMapping("/meters")
    public ResponseEntity<List<SmartMeter>> getMeters(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false) String flat,
            @RequestParam(required = false) String type) {
        AppUser user = loggedInUserService.resolve(principal);
        Long communityId = user.getCommunity().getId();
        return ResponseEntity.ok(smartMeterService.getMeters(communityId, flat, type));
    }

    @GetMapping("/meters/{id}")
    public ResponseEntity<SmartMeter> getMeter(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id) {
        AppUser user = loggedInUserService.resolve(principal);
        Long communityId = user.getCommunity().getId();
        return ResponseEntity.ok(smartMeterService.getMeter(communityId, id));
    }

    @PostMapping("/meters")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    public ResponseEntity<SmartMeter> createMeter(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody SmartMeterRequest request) {
        AppUser user = loggedInUserService.resolve(principal);
        Long communityId = user.getCommunity().getId();
        return ResponseEntity.ok(smartMeterService.createMeter(communityId, request));
    }

    @GetMapping("/meters/{id}/readings")
    public ResponseEntity<List<MeterReading>> getReadings(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        AppUser user = loggedInUserService.resolve(principal);
        Long communityId = user.getCommunity().getId();
        return ResponseEntity.ok(smartMeterService.getReadings(communityId, id, from, to));
    }

    @PostMapping("/meters/{id}/readings")
    public ResponseEntity<MeterReading> addReading(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id,
            @Valid @RequestBody MeterReadingRequest request) {
        AppUser user = loggedInUserService.resolve(principal);
        Long communityId = user.getCommunity().getId();
        return ResponseEntity.ok(smartMeterService.addReading(communityId, id, request));
    }

    @GetMapping("/meters/alerts")
    public ResponseEntity<List<MeterReading>> getAlerts(
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        Long communityId = user.getCommunity().getId();
        return ResponseEntity.ok(smartMeterService.getAlerts(communityId));
    }

    @GetMapping("/meters/community-usage")
    public ResponseEntity<Map<String, Object>> getCommunityUsage(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) Integer month) {
        AppUser user = loggedInUserService.resolve(principal);
        Long communityId = user.getCommunity().getId();
        return ResponseEntity.ok(smartMeterService.getCommunityUsage(communityId, type, month, null));
    }
}
