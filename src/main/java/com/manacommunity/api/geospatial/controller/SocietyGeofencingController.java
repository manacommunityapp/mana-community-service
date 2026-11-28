package com.manacommunity.api.geospatial.controller;

import com.manacommunity.api.geospatial.model.SocietyDetectionRequest;
import com.manacommunity.api.geospatial.model.SocietyDetectionResponse;
import com.manacommunity.api.geospatial.service.SocietyGeofencingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Controller exposing real-time geospatial geofencing and gate routing APIs (< 10ms budget).
 */
@RestController
@RequestMapping("/api/geospatial")
@RequiredArgsConstructor
public class SocietyGeofencingController {

    private final SocietyGeofencingService geofencingService;

    @PostMapping("/detect-society-gate")
    public ResponseEntity<SocietyDetectionResponse> detectSocietyAndGate(
            @RequestBody SocietyDetectionRequest request) {
        SocietyDetectionResponse response = geofencingService.detectSocietyAndNearestGate(request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/detect")
    public ResponseEntity<SocietyDetectionResponse> detectSocietyByQueryParams(
            @RequestParam double lat,
            @RequestParam double lon,
            @RequestParam(required = false, defaultValue = "DELIVERY_PARTNER") String role,
            @RequestParam(required = false, defaultValue = "10.0") Double accuracy) {
        SocietyDetectionRequest request = new SocietyDetectionRequest(lat, lon, role, accuracy);
        SocietyDetectionResponse response = geofencingService.detectSocietyAndNearestGate(request);
        return ResponseEntity.ok(response);
    }
}
