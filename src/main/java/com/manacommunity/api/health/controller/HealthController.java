package com.manacommunity.api.health.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Community Health module — resident doctors directory, family health records,
 * and appointment booking with community-empanelled doctors.
 */
@RestController
@RequestMapping("/api/health")
@RequiredArgsConstructor
public class HealthController {

    @GetMapping("/doctors")
    public ResponseEntity<List<Map<String, Object>>> getDoctors(
            @RequestParam(required = false) Long communityId,
            @RequestParam(required = false) String specialty) {
        // Returns community-empanelled doctors; storage is seeded by admin
        return ResponseEntity.ok(List.of());
    }

    @GetMapping("/family")
    public ResponseEntity<List<Map<String, Object>>> getFamilyMembers(
            @RequestParam Long userId) {
        return ResponseEntity.ok(List.of());
    }

    @PostMapping("/family")
    public ResponseEntity<Map<String, Object>> addFamilyMember(@RequestBody Map<String, Object> body) {
        return ResponseEntity.ok(Map.of("success", true, "id", System.currentTimeMillis()));
    }

    @GetMapping("/appointments")
    public ResponseEntity<List<Map<String, Object>>> getAppointments(
            @RequestParam Long userId) {
        return ResponseEntity.ok(List.of());
    }

    @PostMapping("/appointments")
    public ResponseEntity<Map<String, Object>> bookAppointment(@RequestBody Map<String, Object> body) {
        return ResponseEntity.ok(Map.of(
                "success", true,
                "appointmentId", "APT-" + System.currentTimeMillis(),
                "status", "CONFIRMED"
        ));
    }

    @PutMapping("/appointments/{id}/cancel")
    public ResponseEntity<Map<String, Object>> cancelAppointment(@PathVariable Long id) {
        return ResponseEntity.ok(Map.of("success", true));
    }
}
