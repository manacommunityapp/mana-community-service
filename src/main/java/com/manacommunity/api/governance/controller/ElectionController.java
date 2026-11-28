package com.manacommunity.api.governance.controller;

import com.manacommunity.api.governance.entity.*;
import com.manacommunity.api.governance.repository.*;
import com.manacommunity.api.platform.web.ApiResponse;
import com.manacommunity.api.user.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/governance")
@RequiredArgsConstructor
public class ElectionController {

    private final CommitteePositionRepository positionRepo;
    private final CommitteeElectionRepository electionRepo;
    private final ElectionNominationRepository nominationRepo;
    private final ElectionVoteRepository voteRepo;

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private com.manacommunity.api.governance.service.SecretBallotService secretBallotService;

    // ── Positions ─────────────────────────────────────────────────────────────

    @GetMapping("/positions")
    public ResponseEntity<ApiResponse<List<CommitteePosition>>> getPositions(@AuthenticationPrincipal UserPrincipal principal) {
        Long communityId = principal.getCommunityId();
        return ok(positionRepo.findByCommunityId(communityId));
    }

    @PostMapping("/positions")
    public ResponseEntity<ApiResponse<CommitteePosition>> createPosition(@RequestBody CommitteePosition position) {
        return ok(positionRepo.save(position));
    }

    // ── Elections ─────────────────────────────────────────────────────────────

    @GetMapping("/elections")
    public ResponseEntity<ApiResponse<List<CommitteeElection>>> getElections(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false) String status) {
        Long communityId = principal.getCommunityId();
        List<CommitteeElection> list = status != null
                ? electionRepo.findByCommunityIdAndStatus(communityId, status)
                : electionRepo.findByCommunityIdOrderByCreatedAtDesc(communityId);
        return ok(list);
    }

    @PostMapping("/elections")
    public ResponseEntity<ApiResponse<CommitteeElection>> createElection(@RequestBody CommitteeElection election) {
        election.setStatus("NOMINATIONS_OPEN");
        return ok(electionRepo.save(election));
    }

    @GetMapping("/elections/{id}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getElection(@PathVariable Long id) {
        return electionRepo.findById(id).map(election -> {
            List<ElectionNomination> nominations = nominationRepo.findByElectionId(id);
            List<Object[]> voteCounts = voteRepo.countVotesByNomination(id);
            Map<Long, Long> countMap = voteCounts.stream()
                    .collect(Collectors.toMap(r -> (Long) r[0], r -> (Long) r[1]));

            List<Map<String, Object>> nominationDtos = nominations.stream().map(n -> {
                Map<String, Object> dto = new LinkedHashMap<>();
                dto.put("id", n.getId());
                dto.put("nomineeId", n.getNomineeId());
                dto.put("manifesto", n.getManifesto());
                dto.put("nominatedAt", n.getNominatedAt());
                dto.put("voteCount", countMap.getOrDefault(n.getId(), 0L));
                return dto;
            }).toList();

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("election", election);
            result.put("nominations", nominationDtos);
            result.put("totalVotes", voteRepo.countByElectionId(id));
            return ok(result);
        }).orElse(notFound());
    }

    // ── Nominations ───────────────────────────────────────────────────────────

    @PostMapping("/elections/{id}/nominate")
    @Transactional
    public ResponseEntity<ApiResponse<ElectionNomination>> nominate(
            @PathVariable Long id,
            @RequestBody Map<String, Object> body) {
        return electionRepo.findById(id).map(election -> {
            if (!"NOMINATIONS_OPEN".equals(election.getStatus())) {
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .<ApiResponse<ElectionNomination>>body(ApiResponse.<ElectionNomination>error(
                                "Nominations are not open for this election"));
            }
            Long nomineeId = Long.valueOf(body.get("nomineeId").toString());
            if (nominationRepo.findByElectionIdAndNomineeId(id, nomineeId).isPresent()) {
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .<ApiResponse<ElectionNomination>>body(ApiResponse.<ElectionNomination>error(
                                "Already nominated"));
            }
            ElectionNomination nomination = ElectionNomination.builder()
                    .electionId(id)
                    .nomineeId(nomineeId)
                    .manifesto(body.containsKey("manifesto") ? body.get("manifesto").toString() : null)
                    .build();
            return ok(nominationRepo.save(nomination));
        }).orElse(notFound());
    }

    // ── Voting ────────────────────────────────────────────────────────────────

    @PostMapping("/elections/{id}/vote")
    @Transactional
    public ResponseEntity<ApiResponse<Void>> vote(
            @PathVariable Long id,
            @RequestBody Map<String, Object> body) {
        return electionRepo.findById(id).map(election -> {
            if (!"VOTING_OPEN".equals(election.getStatus())) {
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .<ApiResponse<Void>>body(ApiResponse.error(
                                "Voting is not open for this election"));
            }
            Long voterId = Long.valueOf(body.get("voterId").toString());
            if (voteRepo.findByElectionIdAndVoterId(id, voterId).isPresent()) {
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .<ApiResponse<Void>>body(ApiResponse.error("Already voted"));
            }
            Long nominationId = Long.valueOf(body.get("nominationId").toString());
            try {
                voteRepo.save(ElectionVote.builder()
                        .electionId(id).voterId(voterId).nominationId(nominationId).build());
                return ok((Void) null);
            } catch (DataIntegrityViolationException e) {
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .<ApiResponse<Void>>body(ApiResponse.error("Already voted"));
            }
        }).orElse(notFound());
    }

    // ── Tamper-Evident Secret Ballot (Blind Signatures) ──────────────────────

    @GetMapping("/elections/{id}/public-key")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getElectionPublicKey(@PathVariable Long id) {
        if (secretBallotService == null) return notFound();
        return ok(secretBallotService.getElectionPublicKey(id));
    }

    @PostMapping("/elections/{id}/blind-token")
    public ResponseEntity<ApiResponse<Map<String, Object>>> issueBlindToken(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestBody Map<String, Object> body) {
        if (secretBallotService == null) return notFound();
        Long flatId = Long.valueOf(body.get("flatId").toString());
        Long userId = body.containsKey("userId")
                ? Long.valueOf(body.get("userId").toString())
                : principal.getUserId();
        String blindedMessageHex = body.get("blindedMessageHex").toString();
        return ok(secretBallotService.issueBlindToken(id, flatId, userId, blindedMessageHex));
    }

    @PostMapping("/elections/{id}/secret-ballot")
    public ResponseEntity<ApiResponse<Map<String, Object>>> castSecretBallot(
            @PathVariable Long id,
            @RequestBody Map<String, Object> body) {
        if (secretBallotService == null) return notFound();
        Long nominationId = Long.valueOf(body.get("nominationId").toString());
        String tokenHex = body.get("tokenHex").toString();
        String unblindedSignatureHex = body.get("unblindedSignatureHex").toString();
        return ok(secretBallotService.castSecretBallot(id, nominationId, tokenHex, unblindedSignatureHex));
    }

    @GetMapping("/elections/{id}/audit")
    public ResponseEntity<ApiResponse<Map<String, Object>>> auditSecretBallots(
            @PathVariable Long id,
            @RequestParam(required = false) String receiptCode) {
        if (secretBallotService == null) return notFound();
        return ok(secretBallotService.verifyBallotAuditChain(id, receiptCode));
    }

    @PutMapping("/elections/{id}/status")
    @Transactional
    public ResponseEntity<ApiResponse<CommitteeElection>> updateStatus(
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {
        return electionRepo.findById(id).map(election -> {
            election.setStatus(body.get("status"));
            return ok(electionRepo.save(election));
        }).orElse(notFound());
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private <T> ResponseEntity<ApiResponse<T>> ok(T data) {
        return ResponseEntity.ok(ApiResponse.ok(data));
    }

    @SuppressWarnings("unchecked")
    private <T> ResponseEntity<ApiResponse<T>> notFound() {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body((ApiResponse<T>) ApiResponse.error("Not found"));
    }
}
