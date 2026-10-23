package com.manacommunity.api.polling.engine;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class PollQuorumEngine {

    public enum ResolutionStatus { PASSED, REJECTED, QUORUM_NOT_MET, TIED }

    public record QuorumResult(
            boolean isQuorumMet,
            double participationPercentage,
            int totalEligibleMembers,
            int votesCast,
            double requiredQuorumPercentage
    ) {}

    public record WeightedVote(
            Long optionId,
            double weight // e.g. square footage or share units
    ) {}

    public record ResolutionResult(
            ResolutionStatus status,
            double approvalPercentage,
            double totalWeightedVotes,
            Map<Long, Double> optionTallies
    ) {}

    /**
     * Checks if a poll meets the minimum participation quorum required by community bylaws.
     */
    public QuorumResult evaluateQuorum(int totalEligibleMembers, int votesCast, double requiredQuorumPercentage) {
        if (totalEligibleMembers <= 0) {
            return new QuorumResult(false, 0.0, 0, votesCast, requiredQuorumPercentage);
        }

        double participation = ((double) votesCast / (double) totalEligibleMembers) * 100.0;
        double roundedParticipation = Math.round(participation * 100.0) / 100.0;
        boolean quorumMet = roundedParticipation >= requiredQuorumPercentage;

        return new QuorumResult(quorumMet, roundedParticipation, totalEligibleMembers, votesCast, requiredQuorumPercentage);
    }

    /**
     * Computes weighted tallies where votes are proportional to unit area (sq ft) or share certificates.
     */
    public Map<Long, Double> calculateWeightedTally(List<WeightedVote> votes) {
        if (votes == null || votes.isEmpty()) return Map.of();

        return votes.stream().collect(
                Collectors.groupingBy(
                        WeightedVote::optionId,
                        Collectors.summingDouble(WeightedVote::weight)
                )
        );
    }

    /**
     * Evaluates whether an AGM resolution has passed given majority threshold rules (e.g. 50% simple majority vs 75% special resolution).
     */
    public ResolutionResult evaluateResolution(
            boolean isQuorumMet,
            Map<Long, Double> optionTallies,
            Long yesOptionId,
            Long noOptionId,
            double thresholdPercentage
    ) {
        if (!isQuorumMet) {
            return new ResolutionResult(ResolutionStatus.QUORUM_NOT_MET, 0.0, 0.0, optionTallies);
        }

        double yesTotal = optionTallies.getOrDefault(yesOptionId, 0.0);
        double noTotal = optionTallies.getOrDefault(noOptionId, 0.0);
        double totalDecisive = yesTotal + noTotal;

        if (totalDecisive <= 0.0) {
            return new ResolutionResult(ResolutionStatus.REJECTED, 0.0, 0.0, optionTallies);
        }

        double approval = (yesTotal / totalDecisive) * 100.0;
        double roundedApproval = Math.round(approval * 100.0) / 100.0;

        ResolutionStatus status;
        if (roundedApproval == 50.0 && thresholdPercentage == 50.0) {
            status = ResolutionStatus.TIED;
        } else if (roundedApproval >= thresholdPercentage) {
            status = ResolutionStatus.PASSED;
        } else {
            status = ResolutionStatus.REJECTED;
        }

        return new ResolutionResult(status, roundedApproval, totalDecisive, optionTallies);
    }
}