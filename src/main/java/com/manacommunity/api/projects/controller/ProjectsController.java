package com.manacommunity.api.projects.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Community infrastructure and improvement projects — lifecycle management,
 * milestones, expenses, and metrics. Projects can be created from governance
 * resolutions (POST /from-resolution).
 */
@RestController
@RequestMapping("/api/projects")
@RequiredArgsConstructor
public class ProjectsController {

    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> listProjects(
            @RequestParam(required = false) Long communityId,
            @RequestParam(required = false) String status) {
        return ResponseEntity.ok(List.of());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getProject(@PathVariable Long id) {
        return ResponseEntity.ok(Map.of(
                "id", id,
                "status", "PLANNING",
                "milestones", List.of(),
                "expenses", List.of()
        ));
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> createProject(@RequestBody Map<String, Object> body) {
        return ResponseEntity.ok(Map.of(
                "id", System.currentTimeMillis(),
                "status", "PLANNING"
        ));
    }

    @GetMapping("/metrics")
    public ResponseEntity<Map<String, Object>> getMetrics(
            @RequestParam(required = false) Long communityId) {
        return ResponseEntity.ok(Map.of(
                "totalProjects", 0,
                "activeProjects", 0,
                "completedProjects", 0,
                "totalBudget", 0.0,
                "totalSpent", 0.0
        ));
    }

    @PostMapping("/{projectId}/advance")
    public ResponseEntity<Map<String, Object>> advanceProject(@PathVariable Long projectId) {
        return ResponseEntity.ok(Map.of(
                "id", projectId,
                "status", "IN_PROGRESS",
                "success", true
        ));
    }

    @PostMapping("/from-resolution")
    public ResponseEntity<Map<String, Object>> createFromResolution(@RequestBody Map<String, Object> body) {
        return ResponseEntity.ok(Map.of(
                "id", System.currentTimeMillis(),
                "status", "PLANNING",
                "resolutionId", body.getOrDefault("resolutionId", null),
                "proposalId", body.getOrDefault("proposalId", null)
        ));
    }

    // ── Milestones ────────────────────────────────────────────────────────────

    @PostMapping("/{projectId}/milestones")
    public ResponseEntity<Map<String, Object>> addMilestone(
            @PathVariable Long projectId,
            @RequestBody Map<String, Object> body) {
        return ResponseEntity.ok(Map.of(
                "id", System.currentTimeMillis(),
                "projectId", projectId,
                "status", "PENDING"
        ));
    }

    // ── Expenses ──────────────────────────────────────────────────────────────

    @PostMapping("/{projectId}/expenses")
    public ResponseEntity<Map<String, Object>> addExpense(
            @PathVariable Long projectId,
            @RequestBody Map<String, Object> body) {
        return ResponseEntity.ok(Map.of(
                "id", System.currentTimeMillis(),
                "projectId", projectId,
                "status", "PENDING_APPROVAL"
        ));
    }
}
