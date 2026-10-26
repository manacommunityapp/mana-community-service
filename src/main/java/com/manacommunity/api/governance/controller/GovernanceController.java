package com.manacommunity.api.governance.controller;

import com.manacommunity.api.governance.entity.*;
import com.manacommunity.api.governance.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/governance")
@RequiredArgsConstructor
public class GovernanceController {

    private final GovernanceMeetingRepository meetingRepo;
    private final GovernanceProposalRepository proposalRepo;
    private final GovernanceBallotRepository ballotRepo;
    private final GovernanceResolutionRepository resolutionRepo;
    private final GovernanceVaultDocumentRepository vaultRepo;

    // ── Stats ─────────────────────────────────────────────────────────────────

    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getStats(@RequestParam Long communityId) {
        return ResponseEntity.ok(Map.of(
                "upcomingMeetings", meetingRepo.countByCommunityIdAndStatus(communityId, "SCHEDULED"),
                "activeProposals",  proposalRepo.countByCommunityIdAndStatus(communityId, "OPEN"),
                "openVotes",        ballotRepo.countByCommunityIdAndStatus(communityId, "ACTIVE"),
                "passedResolutions", resolutionRepo.countByCommunityIdAndStatus(communityId, "PASSED")
        ));
    }

    // ── Meetings ──────────────────────────────────────────────────────────────

    @GetMapping("/meetings")
    public ResponseEntity<List<GovernanceMeeting>> getMeetings(
            @RequestParam Long communityId,
            @RequestParam(required = false) String type) {
        List<GovernanceMeeting> list = (type != null && !type.isBlank())
                ? meetingRepo.findByCommunityIdAndTypeOrderByMeetingDateDesc(communityId, type)
                : meetingRepo.findByCommunityIdOrderByMeetingDateDesc(communityId);
        return ResponseEntity.ok(list);
    }

    @GetMapping("/meetings/{id}")
    public ResponseEntity<GovernanceMeeting> getMeeting(@PathVariable Long id) {
        return meetingRepo.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/meetings")
    public ResponseEntity<GovernanceMeeting> createMeeting(@RequestBody GovernanceMeeting meeting) {
        return ResponseEntity.ok(meetingRepo.save(meeting));
    }

    @PostMapping("/meetings/{id}/rsvp")
    @Transactional
    public ResponseEntity<Map<String, Object>> rsvp(
            @PathVariable Long id,
            @RequestBody Map<String, Object> body) {
        return meetingRepo.findById(id).map(m -> {
            m.setQuorumConfirmed(m.getQuorumConfirmed() + 1);
            meetingRepo.save(m);
            return ResponseEntity.ok(Map.<String, Object>of(
                    "success", true,
                    "quorumConfirmed", m.getQuorumConfirmed(),
                    "mode", body.getOrDefault("mode", "PHYSICAL")
            ));
        }).orElse(ResponseEntity.notFound().build());
    }

    // ── Proposals ─────────────────────────────────────────────────────────────

    @GetMapping("/proposals")
    public ResponseEntity<List<GovernanceProposal>> getProposals(
            @RequestParam Long communityId,
            @RequestParam(required = false) String category) {
        List<GovernanceProposal> list = (category != null && !category.isBlank() && !category.equals("ALL"))
                ? proposalRepo.findByCommunityIdAndCategoryOrderByCreatedAtDesc(communityId, category)
                : proposalRepo.findByCommunityIdOrderByCreatedAtDesc(communityId);
        return ResponseEntity.ok(list);
    }

    @PostMapping("/proposals")
    public ResponseEntity<GovernanceProposal> createProposal(@RequestBody GovernanceProposal proposal) {
        return ResponseEntity.ok(proposalRepo.save(proposal));
    }

    @PostMapping("/proposals/{id}/support")
    @Transactional
    public ResponseEntity<Map<String, Object>> supportProposal(@PathVariable Long id) {
        return proposalRepo.findById(id).map(p -> {
            p.setSupportCount(p.getSupportCount() + 1);
            proposalRepo.save(p);
            return ResponseEntity.ok(Map.<String, Object>of(
                    "success", true,
                    "supportCount", p.getSupportCount(),
                    "hasUserSupported", true
            ));
        }).orElse(ResponseEntity.notFound().build());
    }

    // ── Ballots ───────────────────────────────────────────────────────────────

    @GetMapping("/ballots")
    public ResponseEntity<List<GovernanceBallot>> getBallots(
            @RequestParam Long communityId,
            @RequestParam(required = false) String status) {
        List<GovernanceBallot> list = (status != null && !status.isBlank())
                ? ballotRepo.findByCommunityIdAndStatusOrderByCreatedAtDesc(communityId, status)
                : ballotRepo.findByCommunityIdOrderByCreatedAtDesc(communityId);
        return ResponseEntity.ok(list);
    }

    @PostMapping("/ballots/{id}/vote")
    @Transactional
    public ResponseEntity<Map<String, Object>> castVote(
            @PathVariable Long id,
            @RequestBody Map<String, Object> body) {
        return ballotRepo.findById(id).map(b -> {
            b.setVotesCast(b.getVotesCast() + 1);
            ballotRepo.save(b);
            String receiptHash = "VOTE-" + id + "-" + System.currentTimeMillis();
            return ResponseEntity.ok(Map.<String, Object>of("receiptHash", receiptHash));
        }).orElse(ResponseEntity.notFound().build());
    }

    // ── Resolutions ───────────────────────────────────────────────────────────

    @GetMapping("/resolutions")
    public ResponseEntity<List<GovernanceResolution>> getResolutions(
            @RequestParam Long communityId,
            @RequestParam(required = false) String status) {
        return ResponseEntity.ok(resolutionRepo.findByCommunityIdOrderByPassedOnDesc(communityId));
    }

    // ── Vault ─────────────────────────────────────────────────────────────────

    @GetMapping("/vault")
    public ResponseEntity<List<GovernanceVaultDocument>> getVault(
            @RequestParam Long communityId,
            @RequestParam(required = false) String category) {
        List<GovernanceVaultDocument> list = (category != null && !category.isBlank())
                ? vaultRepo.findByCommunityIdAndCategoryOrderByCreatedAtDesc(communityId, category)
                : vaultRepo.findByCommunityIdOrderByCreatedAtDesc(communityId);
        return ResponseEntity.ok(list);
    }

    @PostMapping("/vault")
    public ResponseEntity<GovernanceVaultDocument> uploadVaultDoc(@RequestBody GovernanceVaultDocument doc) {
        return ResponseEntity.ok(vaultRepo.save(doc));
    }
}
