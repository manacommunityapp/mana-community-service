package com.manacommunity.api.governance.controller;

import com.manacommunity.api.governance.dto.GovernanceDtos;
import com.manacommunity.api.governance.dto.GovernanceDtos.*;
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
    public ResponseEntity<StatsDto> getStats(@RequestParam Long communityId) {
        return ResponseEntity.ok(StatsDto.builder()
                .upcomingMeetings(meetingRepo.countByCommunityIdAndStatus(communityId, "SCHEDULED"))
                .activeProposals(proposalRepo.countByCommunityIdAndStatus(communityId, "OPEN"))
                .openVotes(ballotRepo.countByCommunityIdAndStatus(communityId, "ACTIVE"))
                .passedResolutions(resolutionRepo.countByCommunityIdAndStatus(communityId, "PASSED"))
                .actionItemsOpen(0L)
                .myMeetingsAttended(0L)
                .myVotesRecorded(0L)
                .build());
    }

    // ── Meetings ──────────────────────────────────────────────────────────────

    @GetMapping("/meetings")
    public ResponseEntity<List<MeetingDto>> getMeetings(
            @RequestParam Long communityId,
            @RequestParam(required = false) String type) {
        List<GovernanceMeeting> list = (type != null && !type.isBlank())
                ? meetingRepo.findByCommunityIdAndTypeOrderByMeetingDateDesc(communityId, type)
                : meetingRepo.findByCommunityIdOrderByMeetingDateDesc(communityId);
        return ResponseEntity.ok(list.stream().map(this::toMeetingDto).toList());
    }

    @GetMapping("/meetings/{id}")
    public ResponseEntity<MeetingDto> getMeeting(@PathVariable Long id) {
        return meetingRepo.findById(id)
                .map(m -> ResponseEntity.ok(toMeetingDto(m)))
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/meetings")
    public ResponseEntity<MeetingDto> createMeeting(@RequestBody GovernanceMeeting meeting) {
        return ResponseEntity.ok(toMeetingDto(meetingRepo.save(meeting)));
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
    public ResponseEntity<List<ProposalDto>> getProposals(
            @RequestParam Long communityId,
            @RequestParam(required = false) String category) {
        List<GovernanceProposal> list = (category != null && !category.isBlank() && !category.equals("ALL"))
                ? proposalRepo.findByCommunityIdAndCategoryOrderByCreatedAtDesc(communityId, category)
                : proposalRepo.findByCommunityIdOrderByCreatedAtDesc(communityId);
        return ResponseEntity.ok(list.stream().map(this::toProposalDto).toList());
    }

    @PostMapping("/proposals")
    public ResponseEntity<ProposalDto> createProposal(@RequestBody GovernanceProposal proposal) {
        return ResponseEntity.ok(toProposalDto(proposalRepo.save(proposal)));
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
    public ResponseEntity<List<BallotDto>> getBallots(
            @RequestParam Long communityId,
            @RequestParam(required = false) String status) {
        List<GovernanceBallot> list = (status != null && !status.isBlank())
                ? ballotRepo.findByCommunityIdAndStatusOrderByCreatedAtDesc(communityId, status)
                : ballotRepo.findByCommunityIdOrderByCreatedAtDesc(communityId);
        return ResponseEntity.ok(list.stream().map(this::toBallotDto).toList());
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
    public ResponseEntity<List<ResolutionDto>> getResolutions(
            @RequestParam Long communityId,
            @RequestParam(required = false) String status) {
        return ResponseEntity.ok(resolutionRepo.findByCommunityIdOrderByPassedOnDesc(communityId)
                .stream().map(this::toResolutionDto).toList());
    }

    // ── Vault ─────────────────────────────────────────────────────────────────

    @GetMapping("/vault")
    public ResponseEntity<List<VaultDocumentDto>> getVault(
            @RequestParam Long communityId,
            @RequestParam(required = false) String category) {
        List<GovernanceVaultDocument> list = (category != null && !category.isBlank())
                ? vaultRepo.findByCommunityIdAndCategoryOrderByCreatedAtDesc(communityId, category)
                : vaultRepo.findByCommunityIdOrderByCreatedAtDesc(communityId);
        return ResponseEntity.ok(list.stream().map(this::toVaultDto).toList());
    }

    @PostMapping("/vault")
    public ResponseEntity<VaultDocumentDto> uploadVaultDoc(@RequestBody GovernanceVaultDocument doc) {
        return ResponseEntity.ok(toVaultDto(vaultRepo.save(doc)));
    }

    // ── Mappers ───────────────────────────────────────────────────────────────

    private MeetingDto toMeetingDto(GovernanceMeeting m) {
        double pct = m.getQuorumRequired() > 0
                ? Math.round((m.getQuorumConfirmed() * 100.0 / m.getQuorumRequired()) * 10.0) / 10.0
                : 0.0;
        return MeetingDto.builder()
                .id(String.valueOf(m.getId()))
                .type(m.getType())
                .title(m.getTitle())
                .description(m.getDescription())
                .date(m.getMeetingDate() != null ? m.getMeetingDate().toString() : null)
                .time(m.getMeetingTime() != null ? m.getMeetingTime().toString() : null)
                .location(m.getVenue())
                .meetingLink(m.getOnlineLink())
                .status(m.getStatus())
                .quorumConfirmed(m.getQuorumConfirmed())
                .quorumRequired(m.getQuorumRequired())
                .quorumPercentage(pct)
                .isQuorumAchieved(m.getQuorumConfirmed() >= m.getQuorumRequired())
                .hasUserConfirmed(false)
                .attendanceMode("PHYSICAL")
                .agenda(List.of())
                .liveMinutes(List.of())
                .build();
    }

    private ProposalDto toProposalDto(GovernanceProposal p) {
        return ProposalDto.builder()
                .id(String.valueOf(p.getId()))
                .title(p.getTitle())
                .description(p.getDescription())
                .category(p.getCategory())
                .type("PROPOSAL")
                .submittedDate(p.getCreatedAt() != null ? p.getCreatedAt().toLocalDate().toString() : null)
                .supportCount(p.getSupportCount())
                .supportThreshold(10)
                .hasUserSupported(false)
                .status(p.getStatus())
                .pros(List.of())
                .cons(List.of())
                .build();
    }

    private BallotDto toBallotDto(GovernanceBallot b) {
        double turnout = b.getTotalEligible() > 0
                ? Math.round((b.getVotesCast() * 100.0 / b.getTotalEligible()) * 10.0) / 10.0
                : 0.0;
        return BallotDto.builder()
                .id(String.valueOf(b.getId()))
                .title(b.getTitle())
                .description(b.getDescription())
                .startDate(b.getCreatedAt() != null ? b.getCreatedAt().toLocalDate().toString() : null)
                .endDate(b.getCloseAt() != null ? b.getCloseAt().toLocalDate().toString() : null)
                .totalEligibleVoters(b.getTotalEligible())
                .totalVotesCast(b.getVotesCast())
                .turnoutPercentage(turnout)
                .minQuorumPercentage(50)
                .isQuorumReached(turnout >= 50)
                .hasUserVoted(false)
                .status(b.getStatus())
                .options(List.of())
                .build();
    }

    private ResolutionDto toResolutionDto(GovernanceResolution r) {
        return ResolutionDto.builder()
                .id(String.valueOf(r.getId()))
                .resolutionNumber(r.getResolutionNumber())
                .title(r.getTitle())
                .passedDate(r.getPassedOn() != null ? r.getPassedOn().toString() : null)
                .status(r.getStatus())
                .documentUrl(r.getDocumentUrl())
                .votingSummary(Map.of("forVotes", 0, "againstVotes", 0, "abstained", 0, "totalPercentageFor", 0))
                .keyProvisions(List.of())
                .build();
    }

    private VaultDocumentDto toVaultDto(GovernanceVaultDocument d) {
        return VaultDocumentDto.builder()
                .id(String.valueOf(d.getId()))
                .title(d.getTitle())
                .category(d.getCategory())
                .fileFormat(d.getFileType())
                .fileSize(d.getFileSizeKb() != null ? d.getFileSizeKb() + " KB" : null)
                .lastUpdated(d.getUpdatedAt() != null ? d.getUpdatedAt().toLocalDate().toString() : null)
                .documentUrl(d.getDocumentUrl())
                .isConfidential(false)
                .digitalSealVerified(false)
                .build();
    }
}
