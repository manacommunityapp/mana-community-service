package com.manacommunity.api.sports.controller;

import com.manacommunity.api.sports.scheduler.TournamentStageService;
import com.manacommunity.api.sports.scheduler.TournamentStageService.*;
import com.manacommunity.api.user.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/tournament")
@RequiredArgsConstructor
public class TournamentStageController {

    private final TournamentStageService stageService;

    // ─── Stage CRUD ──────────────────────────────────────────────────────────

    @GetMapping("/{configId}/stages")
    @PreAuthorize("hasAnyRole('ADMIN','SPORTS_ADMIN','SUPER_ADMIN')")
    public ResponseEntity<List<StageResponse>> getStages(@PathVariable Long configId) {
        return ResponseEntity.ok(stageService.getStages(configId));
    }

    @PostMapping("/{configId}/stages")
    @PreAuthorize("hasAnyRole('ADMIN','SPORTS_ADMIN','SUPER_ADMIN')")
    public ResponseEntity<StageResponse> createStage(
            @PathVariable Long configId,
            @RequestBody StageCreateRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(stageService.createStage(configId, req));
    }

    @PutMapping("/stages/{stageId}")
    @PreAuthorize("hasAnyRole('ADMIN','SPORTS_ADMIN','SUPER_ADMIN')")
    public ResponseEntity<StageResponse> updateStage(
            @PathVariable Long stageId,
            @RequestBody StageUpdateRequest req) {
        return ResponseEntity.ok(stageService.updateStage(stageId, req));
    }

    @DeleteMapping("/stages/{stageId}")
    @PreAuthorize("hasAnyRole('ADMIN','SPORTS_ADMIN','SUPER_ADMIN')")
    public ResponseEntity<Void> deleteStage(@PathVariable Long stageId) {
        stageService.deleteStage(stageId);
        return ResponseEntity.noContent().build();
    }

    // ─── Schedule Generation ─────────────────────────────────────────────────

    @PostMapping("/stages/{stageId}/schedule/generate")
    @PreAuthorize("hasAnyRole('ADMIN','SPORTS_ADMIN','SUPER_ADMIN')")
    public ResponseEntity<GenerateResult> generateStageSchedule(
            @PathVariable Long stageId,
            @RequestBody(required = false) GenerateRequest req) {
        return ResponseEntity.ok(stageService.generateStageSchedule(stageId, req != null ? req : new GenerateRequest(null, null, null)));
    }

    @PostMapping("/stages/{stageId}/schedule/validate")
    @PreAuthorize("hasAnyRole('ADMIN','SPORTS_ADMIN','SUPER_ADMIN')")
    public ResponseEntity<ValidateResult> validateStageSchedule(@PathVariable Long stageId) {
        return ResponseEntity.ok(stageService.validateStageSchedule(stageId));
    }

    @PostMapping("/stages/{stageId}/schedule/approve")
    @PreAuthorize("hasAnyRole('ADMIN','SPORTS_ADMIN','SUPER_ADMIN')")
    public ResponseEntity<StageResponse> approveStageSchedule(@PathVariable Long stageId) {
        return ResponseEntity.ok(stageService.approveStageSchedule(stageId));
    }

    // ─── Publication ─────────────────────────────────────────────────────────

    @PostMapping("/stages/{stageId}/publish")
    @PreAuthorize("hasAnyRole('ADMIN','SPORTS_ADMIN','SUPER_ADMIN')")
    public ResponseEntity<PublicationResult> publishStage(
            @PathVariable Long stageId,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(stageService.publishStage(stageId, principal.getUsername()));
    }

    @PostMapping("/stages/{stageId}/unpublish")
    @PreAuthorize("hasAnyRole('ADMIN','SPORTS_ADMIN','SUPER_ADMIN')")
    public ResponseEntity<StageResponse> unpublishStage(
            @PathVariable Long stageId,
            @RequestBody Map<String, String> body) {
        return ResponseEntity.ok(stageService.unpublishStage(stageId, body.getOrDefault("reason", "")));
    }

    @PostMapping("/{configId}/publish-stages")
    @PreAuthorize("hasAnyRole('ADMIN','SPORTS_ADMIN','SUPER_ADMIN')")
    public ResponseEntity<List<PublicationResult>> publishMultipleStages(
            @PathVariable Long configId,
            @RequestBody PublishMultipleRequest req,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(stageService.publishMultipleStages(configId, req.stageIds(), principal.getUsername()));
    }

    // ─── Schedule Versions ───────────────────────────────────────────────────

    @GetMapping("/{configId}/schedule/history")
    @PreAuthorize("hasAnyRole('ADMIN','SPORTS_ADMIN','SUPER_ADMIN')")
    public ResponseEntity<List<VersionResponse>> getScheduleVersions(
            @PathVariable Long configId,
            @RequestParam(required = false) Long stageId) {
        return ResponseEntity.ok(stageService.getScheduleVersions(configId, stageId));
    }

    @GetMapping("/schedule/version/{versionId}")
    @PreAuthorize("hasAnyRole('ADMIN','SPORTS_ADMIN','SUPER_ADMIN')")
    public ResponseEntity<VersionResponse> getScheduleVersion(@PathVariable Long versionId) {
        return ResponseEntity.ok(stageService.getScheduleVersion(versionId));
    }

    // ─── Match Postponement ──────────────────────────────────────────────────

    @PostMapping("/matches/{matchId}/postpone")
    @PreAuthorize("hasAnyRole('ADMIN','SPORTS_ADMIN','SUPER_ADMIN')")
    public ResponseEntity<PostponementResponse> postponeMatch(
            @PathVariable Long matchId,
            @RequestBody PostponeRequest req,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(stageService.postponeMatch(matchId, req, principal.getUsername()));
    }

    @GetMapping("/matches/{matchId}/postponements")
    @PreAuthorize("hasAnyRole('ADMIN','SPORTS_ADMIN','SUPER_ADMIN')")
    public ResponseEntity<List<PostponementResponse>> getPostponementHistory(@PathVariable Long matchId) {
        return ResponseEntity.ok(stageService.getPostponementHistory(matchId));
    }

    // ─── Impact Analysis ─────────────────────────────────────────────────────

    @PostMapping("/matches/{matchId}/impact")
    @PreAuthorize("hasAnyRole('ADMIN','SPORTS_ADMIN','SUPER_ADMIN')")
    public ResponseEntity<ImpactResult> getScheduleImpact(
            @PathVariable Long matchId,
            @RequestBody ImpactRequest req) {
        return ResponseEntity.ok(stageService.getScheduleImpact(matchId, req));
    }

    // ─── Reserve Days ────────────────────────────────────────────────────────

    @GetMapping("/{configId}/reserve-days")
    @PreAuthorize("hasAnyRole('ADMIN','SPORTS_ADMIN','SUPER_ADMIN')")
    public ResponseEntity<List<ReserveDayResponse>> getReserveDays(@PathVariable Long configId) {
        return ResponseEntity.ok(stageService.getReserveDays(configId));
    }

    @PostMapping("/{configId}/reserve-days")
    @PreAuthorize("hasAnyRole('ADMIN','SPORTS_ADMIN','SUPER_ADMIN')")
    public ResponseEntity<ReserveDayResponse> createReserveDay(
            @PathVariable Long configId,
            @RequestBody ReserveDayCreateRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(stageService.createReserveDay(configId, req));
    }

    @DeleteMapping("/reserve-days/{reserveDayId}")
    @PreAuthorize("hasAnyRole('ADMIN','SPORTS_ADMIN','SUPER_ADMIN')")
    public ResponseEntity<Void> deleteReserveDay(@PathVariable Long reserveDayId) {
        stageService.deleteReserveDay(reserveDayId);
        return ResponseEntity.noContent().build();
    }

    // ─── Dashboard ───────────────────────────────────────────────────────────

    @GetMapping("/{configId}/schedule/dashboard")
    @PreAuthorize("hasAnyRole('ADMIN','SPORTS_ADMIN','SUPER_ADMIN')")
    public ResponseEntity<DashboardResponse> getScheduleDashboard(@PathVariable Long configId) {
        return ResponseEntity.ok(stageService.getScheduleDashboard(configId));
    }

    // ─── Notifications ───────────────────────────────────────────────────────

    @PostMapping("/stages/{stageId}/notify")
    @PreAuthorize("hasAnyRole('ADMIN','SPORTS_ADMIN','SUPER_ADMIN')")
    public ResponseEntity<NotificationResult> sendStageNotification(
            @PathVariable Long stageId,
            @RequestBody Map<String, String> body) {
        return ResponseEntity.ok(stageService.sendStageNotification(stageId, body.getOrDefault("type", "SCHEDULE_PUBLISHED")));
    }
}
