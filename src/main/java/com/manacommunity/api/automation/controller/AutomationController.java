package com.manacommunity.api.automation.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Community automation engine — rules, delayed task queue, and execution logs.
 * Used by admin panels to configure automated actions (reminders, escalations, etc.).
 */
@RestController
@RequestMapping("/api/automation")
@RequiredArgsConstructor
public class AutomationController {

    @GetMapping("/metrics")
    public ResponseEntity<Map<String, Object>> getMetrics(
            @RequestParam(required = false) Long communityId) {
        return ResponseEntity.ok(Map.of(
                "totalRules", 0,
                "activeRules", 0,
                "executionsToday", 0,
                "failedExecutions", 0,
                "pendingDelayedTasks", 0
        ));
    }

    // ── Rules ─────────────────────────────────────────────────────────────────

    @GetMapping("/rules")
    public ResponseEntity<List<Map<String, Object>>> getRules(
            @RequestParam(required = false) String domain,
            @RequestParam(required = false) Long communityId) {
        return ResponseEntity.ok(List.of());
    }

    @PostMapping("/rules")
    public ResponseEntity<Map<String, Object>> createRule(@RequestBody Map<String, Object> body) {
        return ResponseEntity.ok(Map.of(
                "id", System.currentTimeMillis(),
                "status", "ACTIVE"
        ));
    }

    @PutMapping("/rules/{id}")
    public ResponseEntity<Map<String, Object>> updateRule(
            @PathVariable Long id,
            @RequestBody Map<String, Object> body) {
        return ResponseEntity.ok(Map.of("success", true, "id", id));
    }

    @DeleteMapping("/rules/{id}")
    public ResponseEntity<Map<String, Object>> deleteRule(@PathVariable Long id) {
        return ResponseEntity.ok(Map.of("success", true));
    }

    // ── Delayed tasks ─────────────────────────────────────────────────────────

    @GetMapping("/delayed-tasks")
    public ResponseEntity<List<Map<String, Object>>> getPendingDelayedTasks(
            @RequestParam(required = false) Long communityId) {
        return ResponseEntity.ok(List.of());
    }

    // ── Execution logs ────────────────────────────────────────────────────────

    @GetMapping("/logs")
    public ResponseEntity<List<Map<String, Object>>> getLogs(
            @RequestParam(required = false) Long communityId,
            @RequestParam(required = false) Long ruleId) {
        return ResponseEntity.ok(List.of());
    }
}
