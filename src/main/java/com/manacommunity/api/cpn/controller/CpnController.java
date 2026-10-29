package com.manacommunity.api.cpn.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Community Professional Network — member directory, job board, mentorship bookings,
 * and AI career coach stub.
 */
@RestController
@RequestMapping("/api/cpn")
@RequiredArgsConstructor
public class CpnController {

    @GetMapping("/directory")
    public ResponseEntity<List<Map<String, Object>>> getDirectory(
            @RequestParam(required = false) Long communityId,
            @RequestParam(required = false) String domain) {
        return ResponseEntity.ok(List.of());
    }

    @GetMapping("/jobs")
    public ResponseEntity<List<Map<String, Object>>> getJobs(
            @RequestParam(required = false) Long communityId) {
        return ResponseEntity.ok(List.of());
    }

    @PostMapping("/jobs/{jobId}/referral")
    public ResponseEntity<Map<String, Object>> referJob(@PathVariable Long jobId) {
        return ResponseEntity.ok(Map.of("success", true, "referralId", "REF-" + jobId + "-" + System.currentTimeMillis()));
    }

    @GetMapping("/mentors")
    public ResponseEntity<List<Map<String, Object>>> getMentors(
            @RequestParam(required = false) Long communityId,
            @RequestParam(required = false) String domain) {
        return ResponseEntity.ok(List.of());
    }

    @PostMapping("/mentors/{mentorId}/book")
    public ResponseEntity<Map<String, Object>> bookMentor(
            @PathVariable Long mentorId,
            @RequestBody Map<String, Object> body) {
        return ResponseEntity.ok(Map.of(
                "success", true,
                "sessionId", "MENT-" + mentorId + "-" + System.currentTimeMillis(),
                "status", "PENDING_CONFIRMATION"
        ));
    }

    @GetMapping("/ai-coach")
    public ResponseEntity<Map<String, Object>> aiCoach(@RequestParam Long userId) {
        return ResponseEntity.ok(Map.of(
                "feedback", "Keep building your profile to get personalised AI career coaching.",
                "profileScore", 0,
                "suggestions", List.of()
        ));
    }
}
