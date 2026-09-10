package com.manacommunity.api.service.scheduler;

import com.manacommunity.api.model.SportsAuctionTeam;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Seeding strategy for tournaments: the initial seeding order and the
 * cross-seeding used when group winners feed a knockout bracket.
 */
@Service
public class SportsSeedingService {

    /**
     * Seed teams by ranking. Currently preserves input order as seeding
     * (placeholder for future performance-based seeding).
     */
    public List<SportsAuctionTeam> seed(List<SportsAuctionTeam> teams) {
        return teams;
    }

    /**
     * Standard cross-seeding for group winners: A1 vs B2, B1 vs A2, …
     */
    public List<SportsAuctionTeam> crossSeed(List<SportsAuctionTeam> advancing, int nGroups, int advPer) {
        List<SportsAuctionTeam> result = new ArrayList<>();
        for (int i = 0; i < advPer; i++) {
            for (int g = 0; g < nGroups; g++) {
                int idx = g * advPer + i;
                if (idx < advancing.size()) result.add(advancing.get(idx));
            }
        }
        // Reorder for bracket: A1 vs B2, A2 vs B1
        if (result.size() >= 4) {
            Collections.swap(result, 1, 2);
        }
        return result;
    }
}
