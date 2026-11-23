package com.manacommunity.api.identity.portability.service;

import com.manacommunity.api.identity.portability.dto.ImportResult;
import com.manacommunity.api.identity.portability.entity.CommunityFederationLink;
import com.manacommunity.api.identity.portability.entity.ResidentPortabilityToken;
import com.manacommunity.api.identity.portability.entity.ResidentReputationScore;
import com.manacommunity.api.identity.portability.repository.CommunityFederationLinkRepository;
import com.manacommunity.api.identity.portability.repository.ResidentPortabilityTokenRepository;
import com.manacommunity.api.identity.portability.repository.ResidentReputationScoreRepository;
import com.manacommunity.api.user.model.AppUser;
import com.manacommunity.api.user.repository.AppUserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class ResidentPortabilityService {

    private final ResidentPortabilityTokenRepository tokenRepo;
    private final ResidentReputationScoreRepository reputationRepo;
    private final CommunityFederationLinkRepository federationRepo;
    private final AppUserRepository userRepo;

    // ── Portability token ────────────────────────────────────────────────────

    @Transactional
    public ResidentPortabilityToken generatePortabilityToken(Long residentId, Long communityId) {
        // Reuse unexpired token if one exists
        return tokenRepo
                .findTopByResidentIdAndSourceCommunityIdAndExpiresAtAfterOrderByCreatedAtDesc(
                        residentId, communityId, Instant.now())
                .orElseGet(() -> buildNewToken(residentId, communityId));
    }

    private ResidentPortabilityToken buildNewToken(Long residentId, Long communityId) {
        ResidentReputationScore rep = reputationRepo
                .findByResidentIdAndCommunityId(residentId, communityId)
                .orElseGet(() -> computeReputationScore(residentId, communityId));

        AppUser user = userRepo.findById(residentId).orElseThrow();

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("residentId", residentId);
        payload.put("name", user.getName());
        payload.put("reputation_score", rep.getScore());
        payload.put("tenure_months", rep.getTenureMonths());
        payload.put("badges", rep.getBadges() != null ? rep.getBadges() : List.of());
        payload.put("payment_score", rep.getPaymentScore());
        payload.put("participation_score", rep.getParticipation());
        payload.put("generated_at", Instant.now().toString());

        String rawToken = UUID.randomUUID().toString() + residentId + System.currentTimeMillis();
        String tokenHash = sha256(rawToken);

        return tokenRepo.save(ResidentPortabilityToken.builder()
                .residentId(residentId)
                .sourceCommunityId(communityId)
                .tokenHash(tokenHash)
                .exportPayload(payload)
                .expiresAt(Instant.now().plus(30, ChronoUnit.DAYS))
                .build());
    }

    @Transactional
    public ImportResult importResidentProfile(Long targetCommunityId, String tokenHash) {
        ResidentPortabilityToken token = tokenRepo
                .findByTokenHashAndExpiresAtAfterAndClaimedAtIsNull(tokenHash, Instant.now())
                .orElseThrow(() -> new IllegalArgumentException("Token not found, expired, or already claimed"));

        // Mark claimed
        token.setClaimedAt(Instant.now());
        token.setClaimedByCommunity(targetCommunityId);
        tokenRepo.save(token);

        Map<String, Object> payload = token.getExportPayload();
        boolean kycAutoVerified = Boolean.TRUE.equals(payload.get("kyc_verified"));
        Double repScore = payload.get("reputation_score") instanceof Number n ? n.doubleValue() : 50.0;

        // Seed reputation score in target community
        Long residentId = token.getResidentId();
        ResidentReputationScore targetRep = reputationRepo
                .findByResidentIdAndCommunityId(residentId, targetCommunityId)
                .orElseGet(() -> ResidentReputationScore.builder()
                        .residentId(residentId).communityId(targetCommunityId).build());

        targetRep.setScore(BigDecimal.valueOf(repScore).setScale(2, RoundingMode.HALF_UP));
        targetRep.setComputedAt(Instant.now());
        reputationRepo.save(targetRep);

        log.info("Imported resident {} profile into community {}: kycAutoVerified={}, repScore={}",
                residentId, targetCommunityId, kycAutoVerified, repScore);

        return ImportResult.builder()
                .residentId(residentId)
                .kycAutoVerified(kycAutoVerified)
                .reputationScoreCopied(repScore)
                .importedPayload(payload)
                .message("Resident profile imported successfully")
                .build();
    }

    public Optional<ResidentPortabilityToken> getMyToken(Long residentId, Long communityId) {
        return tokenRepo.findTopByResidentIdAndSourceCommunityIdAndExpiresAtAfterOrderByCreatedAtDesc(
                residentId, communityId, Instant.now());
    }

    // ── Reputation ───────────────────────────────────────────────────────────

    @Transactional
    public ResidentReputationScore computeReputationScore(Long residentId, Long communityId) {
        AppUser user = userRepo.findById(residentId).orElseThrow();

        // Tenure: months since account created (proxy for community join)
        long tenureMonths = ChronoUnit.MONTHS.between(
                user.getCreatedAt().toLocalDate(),
                LocalDate.now());

        // Participation score: 0–100 based on tenure (simple baseline; enriched over time)
        BigDecimal participation = BigDecimal.valueOf(Math.min(tenureMonths * 2, 100));

        // Payment score: default 100 until payment data is integrated
        BigDecimal paymentScore = BigDecimal.valueOf(100);

        // Composite: 60% payment + 40% participation
        BigDecimal composite = paymentScore.multiply(BigDecimal.valueOf(0.6))
                .add(participation.multiply(BigDecimal.valueOf(0.4)))
                .setScale(2, RoundingMode.HALF_UP);

        List<Map<String, Object>> badges = new ArrayList<>();
        if (tenureMonths >= 12) badges.add(Map.of("type", "TENURE_1Y", "label", "1 Year Resident", "earned_at", Instant.now().toString()));
        if (tenureMonths >= 36) badges.add(Map.of("type", "TENURE_3Y", "label", "3 Year Resident", "earned_at", Instant.now().toString()));

        ResidentReputationScore score = reputationRepo
                .findByResidentIdAndCommunityId(residentId, communityId)
                .orElseGet(() -> ResidentReputationScore.builder()
                        .residentId(residentId).communityId(communityId).build());

        score.setScore(composite);
        score.setPaymentScore(paymentScore);
        score.setParticipation(participation);
        score.setTenureMonths((int) tenureMonths);
        score.setBadges(badges);
        score.setComputedAt(Instant.now());

        return reputationRepo.save(score);
    }

    public Optional<ResidentReputationScore> getMyScore(Long residentId, Long communityId) {
        return reputationRepo.findByResidentIdAndCommunityId(residentId, communityId);
    }

    @Scheduled(cron = "0 3 1 * *")
    public void recomputeAllScores() {
        log.info("Scheduled: recomputing all resident reputation scores");
        // Recompute per existing score records
        reputationRepo.findAll().forEach(r ->
                computeReputationScore(r.getResidentId(), r.getCommunityId()));
    }

    // ── Federation ───────────────────────────────────────────────────────────

    @Transactional
    public CommunityFederationLink initiateFederation(Long communityId, Long targetCommunityId,
                                                       String linkType, Long initiatedBy) {
        // Canonical ordering: smaller id always goes in communityIdA
        Long a = Math.min(communityId, targetCommunityId);
        Long b = Math.max(communityId, targetCommunityId);
        String type = linkType != null ? linkType : "IDENTITY_SHARE";

        return federationRepo.save(CommunityFederationLink.builder()
                .communityIdA(a)
                .communityIdB(b)
                .linkType(type)
                .initiatedBy(initiatedBy)
                .build());
    }

    @Transactional
    public CommunityFederationLink approveFederation(Long linkId, Long approvedBy) {
        CommunityFederationLink link = federationRepo.findById(linkId)
                .orElseThrow(() -> new NoSuchElementException("Federation link not found: " + linkId));
        link.setStatus("ACTIVE");
        link.setApprovedBy(approvedBy);
        link.setApprovedAt(Instant.now());
        return federationRepo.save(link);
    }

    public List<CommunityFederationLink> getFederatedCommunities(Long communityId) {
        return federationRepo.findAllByCommunity(communityId);
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private static String sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : hash) hex.append(String.format("%02x", b));
            return hex.toString();
        } catch (Exception e) {
            throw new RuntimeException("SHA-256 failed", e);
        }
    }
}
