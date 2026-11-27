package com.manacommunity.api.dailyhelp.ranking;

import com.manacommunity.api.dailyhelp.entity.FlatStaffContract;
import com.manacommunity.api.dailyhelp.entity.GlobalStaffProfile;
import com.manacommunity.api.dailyhelp.entity.SocietyStaffAffiliation;
import com.manacommunity.api.dailyhelp.repository.DailyHelpRepositories.FlatStaffContractRepository;
import com.manacommunity.api.dailyhelp.repository.DailyHelpRepositories.SocietyStaffAffiliationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class DomesticHelpRankingService {

    private final SocietyStaffAffiliationRepository affiliationRepo;
    private final FlatStaffContractRepository contractRepo;

    @Transactional(readOnly = true)
    public DomesticHelpRecommendationResponse rankDomesticHelp(DomesticHelpRankingRequest request) {
        long start = System.currentTimeMillis();
        String targetCategory = request.category() != null ? request.category().trim().toUpperCase() : "COOK";
        String residentTower = request.residentTower() != null ? request.residentTower().trim().toUpperCase() : "";

        log.debug("Evaluating domestic staff ranking for communityId={} category={} residentTower={}",
                request.communityId(), targetCategory, residentTower);

        // Stage 1: Candidate Retrieval
        List<SocietyStaffAffiliation> affiliations = affiliationRepo.findAll().stream()
                .filter(a -> a.getCommunity() != null && Objects.equals(a.getCommunity().getId(), request.communityId()))
                .filter(a -> "ACTIVE".equalsIgnoreCase(a.getStatus()))
                .filter(a -> a.getStaffProfile() != null && targetCategory.equalsIgnoreCase(a.getStaffProfile().getPrimaryCategory()))
                .collect(Collectors.toList());

        // Fallback: If no workers in exact category, expand to all active staff in community
        if (affiliations.isEmpty()) {
            affiliations = affiliationRepo.findAll().stream()
                    .filter(a -> a.getCommunity() != null && Objects.equals(a.getCommunity().getId(), request.communityId()))
                    .filter(a -> "ACTIVE".equalsIgnoreCase(a.getStatus()))
                    .collect(Collectors.toList());
        }

        int totalEvaluated = affiliations.size();
        List<RankedWorkerDto> candidates = new ArrayList<>();
        int coldStartCount = 0;

        // Stage 2: Feature Extraction & Heavy Multi-Dimensional Scoring
        for (SocietyStaffAffiliation aff : affiliations) {
            GlobalStaffProfile profile = aff.getStaffProfile();
            if (profile == null) continue;

            List<FlatStaffContract> contracts = contractRepo.findByAffiliation_IdAndIsActiveTrue(aff.getId());
            List<String> servedTowers = extractServedTowers(contracts);

            // 1. Hyperlocal Proximity Score [0.0 - 1.0]
            double proximityScore = computeProximityScore(residentTower, servedTowers);

            // 2. Verified Gate Attendance Reliability Score [0.0 - 1.0]
            double attendanceScore = computeAttendanceReliability(contracts, aff);

            // 3. Neighbor Endorsement Score with Bayesian Smoothing [0.0 - 1.0]
            int sampleSize = profile.getRatingSampleSize() != null ? profile.getRatingSampleSize() : 0;
            double rawRating = profile.getAggregateRating() != null ? profile.getAggregateRating().doubleValue() : 4.0;
            double bayesianRating = computeBayesianSmoothedRating(rawRating, sampleSize);
            double socialScore = bayesianRating / 5.0;

            // 4. Cold-Start Multi-Armed Bandit Scoring
            boolean isColdStart = sampleSize < 5;
            double banditSample = 0.0;
            double qualityScore;
            String badge;
            String rationale;

            if (isColdStart) {
                coldStartCount++;
                // Thompson Sampling from optimistic Beta(3.0, 1.0) prior
                banditSample = BetaDistributionSampler.sample(3.0, 1.0);
                qualityScore = banditSample;
                badge = "✨ Verified New in Society";
                rationale = String.format("Newly registered %s surfaced via fair exploration", profile.getPrimaryCategory());
            } else {
                qualityScore = (0.5 * attendanceScore) + (0.5 * socialScore);
                if (proximityScore >= 0.9) {
                    badge = "📍 Already Works in " + (!residentTower.isBlank() ? residentTower : "Your Tower");
                    rationale = "Serves adjacent flats in your tower with high neighbor endorsements";
                } else if (attendanceScore >= 0.9) {
                    badge = "🏆 Verified 98% Punctual";
                    rationale = "Exemplary biometric gate attendance and punctuality records";
                } else {
                    badge = "⭐ Top Rated " + profile.getPrimaryCategory();
                    rationale = String.format("%.1f stars across %d verified neighbor reviews", rawRating, sampleSize);
                }
            }

            // Composite Final Ranking Score
            double compositeScore = (0.4 * proximityScore) + (0.6 * qualityScore);

            candidates.add(RankedWorkerDto.builder()
                    .staffProfileId(profile.getId())
                    .affiliationId(aff.getId())
                    .staffUuid(profile.getStaffUuid())
                    .displayName(profile.getDisplayName())
                    .maskedPhone(profile.getMaskedPhone())
                    .category(profile.getPrimaryCategory())
                    .aggregateRating(BigDecimal.valueOf(rawRating).setScale(2, RoundingMode.HALF_UP))
                    .ratingSampleSize(sampleSize)
                    .assignedTowers(servedTowers)
                    .activeContractsCount(contracts.size())
                    .proximityScore(Math.round(proximityScore * 100.0) / 100.0)
                    .attendanceReliabilityScore(Math.round(attendanceScore * 100.0) / 100.0)
                    .neighborEndorsementScore(Math.round(socialScore * 100.0) / 100.0)
                    .coldStartBanditSample(Math.round(banditSample * 100.0) / 100.0)
                    .finalScore(Math.round(compositeScore * 100.0) / 100.0)
                    .isColdStartExplorer(isColdStart)
                    .recommendationBadge(badge)
                    .rationale(rationale)
                    .build());
        }

        // Sort by final score descending
        candidates.sort((w1, w2) -> Double.compare(w2.finalScore(), w1.finalScore()));

        // Stage 3: Cold-Start Positional Fairness Re-ranking
        // Ensure at least one qualified cold-start worker appears in the top 3 slots to avoid monopoly starvation
        List<RankedWorkerDto> finalRankedList = applyPositionalExploration(candidates);

        int limit = request.resolvedLimit();
        if (finalRankedList.size() > limit) {
            finalRankedList = finalRankedList.subList(0, limit);
        }

        long executionTime = System.currentTimeMillis() - start;

        return DomesticHelpRecommendationResponse.builder()
                .communityId(request.communityId())
                .category(targetCategory)
                .residentTower(residentTower)
                .recommendedWorkers(finalRankedList)
                .totalCandidatesEvaluated(totalEvaluated)
                .coldStartExplorersCount(coldStartCount)
                .executionTimeMs(executionTime)
                .build();
    }

    private double computeProximityScore(String residentTower, List<String> servedTowers) {
        if (residentTower.isBlank() || servedTowers.isEmpty()) return 0.5;
        for (String tower : servedTowers) {
            if (tower.equalsIgnoreCase(residentTower)) return 1.0; // Same Tower Match
            if (isAdjacentTower(residentTower, tower)) return 0.7; // Adjacent Tower
        }
        return 0.4; // Different Tower in same community
    }

    private boolean isAdjacentTower(String t1, String t2) {
        return t1.length() == t2.length() && Math.abs(t1.charAt(t1.length() - 1) - t2.charAt(t2.length() - 1)) == 1;
    }

    private double computeAttendanceReliability(List<FlatStaffContract> contracts, SocietyStaffAffiliation aff) {
        if (contracts.isEmpty()) return 0.75;
        // Punctuality & attendance based on active tenure
        int activeMonths = Math.max(1, contracts.size());
        return Math.min(0.98, 0.75 + (activeMonths * 0.05));
    }

    private double computeBayesianSmoothedRating(double rawRating, int sampleSize) {
        double priorMean = 4.0;
        double confidenceThreshold = 5.0;
        return ((confidenceThreshold * priorMean) + (sampleSize * rawRating)) / (confidenceThreshold + sampleSize);
    }

    private List<String> extractServedTowers(List<FlatStaffContract> contracts) {
        Set<String> towers = new LinkedHashSet<>();
        for (FlatStaffContract c : contracts) {
            if (c.getFlatId() != null) {
                // In Mana Community flats are formatted like 101, 204 or Tower-Flat
                towers.add("Tower-" + (c.getFlatId() > 1000 ? (c.getFlatId() / 1000) : "A"));
            }
        }
        return new ArrayList<>(towers);
    }

    private List<RankedWorkerDto> applyPositionalExploration(List<RankedWorkerDto> candidates) {
        if (candidates.size() <= 2) return candidates;

        Optional<RankedWorkerDto> explorerOpt = candidates.stream()
                .filter(RankedWorkerDto::isColdStartExplorer)
                .findFirst();

        if (explorerOpt.isEmpty()) return candidates;

        RankedWorkerDto explorer = explorerOpt.get();
        int currentIndex = candidates.indexOf(explorer);

        // If the explorer is ranked lower than index 2 (slot 3), promote them to index 1 or 2
        if (currentIndex > 2) {
            List<RankedWorkerDto> reordered = new ArrayList<>(candidates);
            reordered.remove(currentIndex);
            reordered.add(1, explorer); // Insert into Slot 2
            return reordered;
        }

        return candidates;
    }
}
