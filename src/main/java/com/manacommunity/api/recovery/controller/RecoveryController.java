package com.manacommunity.api.recovery.controller;

import com.manacommunity.api.recovery.dto.RecoveryDtos.*;
import com.manacommunity.api.recovery.service.GstAdaptiveBackfillService;
import com.manacommunity.api.recovery.service.RfidPitrRecoveryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/recovery")
@RequiredArgsConstructor
public class RecoveryController {

    private final GstAdaptiveBackfillService backfillService;
    private final RfidPitrRecoveryService rfidRecoveryService;

    // ── Safe Production GST Backfill (AIMD Lag Throttling) ───────────────────

    @PostMapping("/backfill/dry-run")
    public ResponseEntity<GstBackfillJobDto> stageDryRun(@RequestBody StageDryRunRequest request) {
        return ResponseEntity.ok(backfillService.stageDryRunManifest(request));
    }

    @PostMapping("/backfill/seed-demo")
    public ResponseEntity<GstBackfillJobDto> seedDemoDryRun(@RequestParam(required = false) String incidentRef) {
        return ResponseEntity.ok(backfillService.seedIncidentDryRunDemo(incidentRef));
    }

    @PostMapping("/backfill/chunk")
    public ResponseEntity<AdaptiveChunkResultDto> executeAdaptiveChunk(
            @RequestBody ExecuteAdaptiveChunkRequest request) {
        return ResponseEntity.ok(backfillService.executeAdaptiveChunk(request));
    }

    @GetMapping("/backfill/jobs")
    public ResponseEntity<List<GstBackfillJobDto>> listBackfillJobs() {
        return ResponseEntity.ok(backfillService.listJobs());
    }

    @GetMapping("/backfill/jobs/{jobId}/items")
    public ResponseEntity<List<GstBackfillItemDto>> listJobItems(@PathVariable UUID jobId) {
        return ResponseEntity.ok(backfillService.listJobItems(jobId));
    }

    // ── RFID Bulk Corruption Mitigation, PITR Restore & Guardrails ───────────

    @PostMapping("/rfid/simulate-corruption")
    public ResponseEntity<List<RfidTagDto>> simulate815AmCorruption(
            @RequestParam long societyId,
            @RequestParam(required = false) Instant incidentTimestamp) {
        return ResponseEntity.ok(rfidRecoveryService.simulate815AmCorruption(societyId, incidentTimestamp));
    }

    @PostMapping("/rfid/emergency-override")
    public ResponseEntity<EmergencyOverrideDto> toggleEmergencyOverride(
            @RequestBody EmergencyOverrideRequest request) {
        return ResponseEntity.ok(rfidRecoveryService.toggleEmergencyOverride(request));
    }

    @GetMapping("/rfid/gate-evaluate")
    public ResponseEntity<GateEvaluationDto> evaluateGateEntry(
            @RequestParam long societyId,
            @RequestParam String tagId) {
        return ResponseEntity.ok(rfidRecoveryService.evaluateGateEntry(societyId, tagId));
    }

    @PostMapping("/rfid/pitr-restore")
    public ResponseEntity<SurgicalPitrRestoreResultDto> executeSurgicalPitrRestore(
            @RequestParam long societyId,
            @RequestParam(required = false) Instant sinceTs,
            @RequestParam(required = false) String actorName) {
        return ResponseEntity.ok(rfidRecoveryService.executeSurgicalPitrRestore(societyId, sinceTs, actorName));
    }

    @PostMapping("/rfid/guardrail-check")
    public ResponseEntity<BulkMutationGuardrailDto> checkBulkGuardrail(
            @RequestBody BulkMutationCheckRequest request) {
        return ResponseEntity.ok(rfidRecoveryService.attemptGuardedBulkMutation(request));
    }

    @GetMapping("/rfid/tags")
    public ResponseEntity<List<RfidTagDto>> listTags(@RequestParam long societyId) {
        return ResponseEntity.ok(rfidRecoveryService.listTags(societyId));
    }

    @GetMapping("/rfid/guardrails")
    public ResponseEntity<List<BulkMutationGuardrailDto>> listGuardrails(@RequestParam long societyId) {
        return ResponseEntity.ok(rfidRecoveryService.listGuardrails(societyId));
    }
}
