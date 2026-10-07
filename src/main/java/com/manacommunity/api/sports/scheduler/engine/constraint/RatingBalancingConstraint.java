package com.manacommunity.api.sports.scheduler.engine.constraint;

import com.manacommunity.api.sports.scheduler.engine.dto.ParticipantNode;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class RatingBalancingConstraint {

    /**
     * Sort and assign seeds by rating/Elo if seeds are not explicitly defined.
     */
    public List<ParticipantNode> balanceAndSeed(List<ParticipantNode> participants) {
        List<ParticipantNode> sorted = new ArrayList<>(participants);
        sorted.sort((a, b) -> {
            Double rA = a.getRating() != null ? a.getRating() : 0.0;
            Double rB = b.getRating() != null ? b.getRating() : 0.0;
            return Double.compare(rB, rA); // Highest rating first
        });

        for (int i = 0; i < sorted.size(); i++) {
            if (sorted.get(i).getSeed() == null || sorted.get(i).getSeed() <= 0) {
                sorted.get(i).setSeed(i + 1);
            }
        }
        return sorted;
    }
}
