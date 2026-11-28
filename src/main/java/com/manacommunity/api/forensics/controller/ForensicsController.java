package com.manacommunity.api.forensics.controller;

import com.manacommunity.api.forensics.dto.ForensicsDtos.*;
import com.manacommunity.api.forensics.service.BitemporalLedgerService;
import com.manacommunity.api.forensics.service.WormAuditAnchorService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/forensics")
@RequiredArgsConstructor
public class ForensicsController {

    private final BitemporalLedgerService ledgerService;
    private final WormAuditAnchorService wormAuditService;

    // ── Bitemporal Event-Sourced Ledger & Point-in-Time Reconstruction ───────

    @PostMapping("/ledger/events")
    public ResponseEntity<BitemporalLedgerEventDto> appendLedgerEvent(
            @RequestBody AppendLedgerEventRequest request) {
        return ResponseEntity.ok(ledgerService.appendEvent(request));
    }

    @GetMapping("/ledger/events")
    public ResponseEntity<List<BitemporalLedgerEventDto>> listFlatEvents(
            @RequestParam long societyId,
            @RequestParam long flatId) {
        return ResponseEntity.ok(ledgerService.listFlatEvents(societyId, flatId));
    }

    @PostMapping("/ledger/checkpoints")
    public ResponseEntity<LedgerCheckpointDto> createCheckpoint(
            @RequestParam long societyId,
            @RequestParam long flatId,
            @RequestParam LocalDate checkpointDate,
            @RequestParam(required = false) Instant recordedCutoff) {
        return ResponseEntity.ok(ledgerService.createCheckpoint(societyId, flatId, checkpointDate, recordedCutoff));
    }

    @PostMapping("/ledger/time-travel")
    public ResponseEntity<TimeTravelReconstructionDto> reconstructAtPointInTime(
            @RequestBody TimeTravelQueryRequest request) {
        return ResponseEntity.ok(ledgerService.reconstructAtPointInTime(request));
    }

    @PostMapping("/ledger/seed-b902")
    public ResponseEntity<TimeTravelReconstructionDto> seedB902Scenario(@RequestParam long societyId) {
        return ResponseEntity.ok(ledgerService.seedFlatB902Scenario(societyId));
    }

    // ── Tamper-Proof WORM Audit Logs & External Merkle Anchoring ────────────

    @PostMapping("/audit/events")
    public ResponseEntity<WormAuditEventDto> recordSensitiveAction(
            @RequestBody RecordSensitiveAuditRequest request) {
        return ResponseEntity.ok(wormAuditService.recordSensitiveAction(request));
    }

    @GetMapping("/audit/events")
    public ResponseEntity<List<WormAuditEventDto>> listAuditEvents(@RequestParam long societyId) {
        return ResponseEntity.ok(wormAuditService.listAuditEvents(societyId));
    }

    @PostMapping("/audit/anchors")
    public ResponseEntity<ExternalMerkleAnchorDto> anchorMerkleBatch(@RequestParam long societyId) {
        return ResponseEntity.ok(wormAuditService.anchorMerkleBatch(societyId));
    }

    @GetMapping("/audit/anchors")
    public ResponseEntity<List<ExternalMerkleAnchorDto>> listAnchors(@RequestParam long societyId) {
        return ResponseEntity.ok(wormAuditService.listAnchors(societyId));
    }

    @GetMapping("/audit/verify")
    public ResponseEntity<ForensicVerificationReportDto> verifyIntegrity(@RequestParam long societyId) {
        return ResponseEntity.ok(wormAuditService.verifyIntegrity(societyId));
    }

    @PostMapping("/audit/simulate-tamper")
    public ResponseEntity<ForensicVerificationReportDto> simulateRogueDbaTamper(
            @RequestBody SimulateDbaTamperRequest request) {
        return ResponseEntity.ok(wormAuditService.simulateRogueDbaTamper(request));
    }
}
