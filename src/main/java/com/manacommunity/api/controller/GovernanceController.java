package com.manacommunity.api.controller;

import com.manacommunity.api.dto.*;
import com.manacommunity.api.service.GovernanceService;
import com.manacommunity.api.user.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/governance")
@RequiredArgsConstructor
public class GovernanceController {

    private final GovernanceService governanceService;

    // ── Stats ─────────────────────────────────────────────────────────────────

    @GetMapping("/stats")
    public ResponseEntity<GovernanceStatsResponse> getStats(
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(governanceService.getStats(
                principal.getCommunityId(), principal.getId()));
    }

    // ── Meetings ──────────────────────────────────────────────────────────────

    @GetMapping("/meetings")
    public ResponseEntity<List<MeetingResponse>> getMeetings(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false) String type) {
        return ResponseEntity.ok(governanceService.getMeetings(
                principal.getCommunityId(), principal.getId(), type));
    }

    @GetMapping("/meetings/{id}")
    public ResponseEntity<MeetingResponse> getMeetingById(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id) {
        return ResponseEntity.ok(governanceService.getMeetingById(
                principal.getCommunityId(), principal.getId(), id));
    }

    @PostMapping("/meetings/{id}/rsvp")
    public ResponseEntity<Void> confirmAttendance(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {
        governanceService.confirmAttendance(
                principal.getCommunityId(), principal.getId(), id,
                body.get("mode"), body.get("proxyNominee"));
        return ResponseEntity.ok().build();
    }

    // ── Proposals ─────────────────────────────────────────────────────────────

    @GetMapping("/proposals")
    public ResponseEntity<List<ProposalResponse>> getProposals(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false) String category) {
        return ResponseEntity.ok(governanceService.getProposals(
                principal.getCommunityId(), principal.getId(), category));
    }

    @PostMapping("/proposals")
    public ResponseEntity<ProposalResponse> createProposal(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestBody ProposalRequest request) {
        return ResponseEntity.ok(governanceService.createProposal(
                principal.getCommunityId(), principal.getId(), request));
    }

    @PostMapping("/proposals/{id}/support")
    public ResponseEntity<ProposalSupportResponse> supportProposal(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id) {
        return ResponseEntity.ok(governanceService.supportProposal(
                principal.getCommunityId(), principal.getId(), id));
    }

    // ── Ballots ───────────────────────────────────────────────────────────────

    @GetMapping("/ballots")
    public ResponseEntity<List<BallotResponse>> getBallots(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false) String status) {
        return ResponseEntity.ok(governanceService.getBallots(
                principal.getCommunityId(), principal.getId(), status));
    }

    @PostMapping("/ballots/{id}/vote")
    public ResponseEntity<BallotVoteResponse> castVote(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {
        return ResponseEntity.ok(governanceService.castVote(
                principal.getCommunityId(), principal.getId(), id, body.get("optionId")));
    }

    // ── Resolutions ───────────────────────────────────────────────────────────

    @GetMapping("/resolutions")
    public ResponseEntity<List<ResolutionResponse>> getResolutions(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false) String search) {
        return ResponseEntity.ok(governanceService.getResolutions(
                principal.getCommunityId(), search));
    }

    // ── Vault ─────────────────────────────────────────────────────────────────

    @GetMapping("/vault")
    public ResponseEntity<List<VaultDocumentResponse>> getVaultDocuments(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String search) {
        return ResponseEntity.ok(governanceService.getVaultDocuments(
                principal.getCommunityId(), category, search));
    }
}
