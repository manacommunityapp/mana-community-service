package com.manacommunity.api.sports.scheduler.engine.constraint;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class BalancedBracketConstraint {

    /**
     * Generate standard ITF/BWF seeded draw bracket order for size N (power of 2).
     * e.g. for N=8: [1, 8, 4, 5, 2, 7, 3, 6]
     */
    public List<Integer> generateStandardSeedPositions(int bracketSize) {
        List<Integer> rounds = new ArrayList<>();
        rounds.add(1);
        rounds.add(2);

        while (rounds.size() < bracketSize) {
            List<Integer> next = new ArrayList<>();
            int sum = rounds.size() * 2 + 1;
            for (int seed : rounds) {
                next.add(seed);
                next.add(sum - seed);
            }
            rounds = next;
        }
        return rounds;
    }

    public String roundName(int roundIndex, int totalRounds) {
        int roundsRemaining = totalRounds - roundIndex;
        return switch (roundsRemaining) {
            case 1 -> "Final";
            case 2 -> "Semi-Finals";
            case 3 -> "Quarter-Finals";
            case 4 -> "Round of 16";
            case 5 -> "Round of 32";
            case 6 -> "Round of 64";
            default -> "Round " + (roundIndex + 1);
        };
    }
}
