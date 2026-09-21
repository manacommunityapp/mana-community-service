package com.manacommunity.api.service.scheduler;

import com.manacommunity.api.model.SportsAuctionPlayer;
import com.manacommunity.api.model.SportsAuctionTeam;
import com.manacommunity.api.model.SportsPlayerRanking;
import com.manacommunity.api.repository.SportsPlayerRankingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * Seeding strategy for tournaments: the initial seeding order and the
 * cross-seeding used when group winners feed a knockout bracket.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SportsSeedingService {

    private final SportsPlayerRankingRepository rankingRepo;

    /**
     * Seed teams by player ranking / rating.
     */
    public List<SportsAuctionTeam> seed(List<SportsAuctionTeam> teams, Long sportId, Long communityId, String season) {
        if (teams == null || teams.size() < 2 || sportId == null || communityId == null) {
            return teams != null ? teams : Collections.emptyList();
        }

        String resolvedSeason = (season == null || season.isBlank()) ? "CURRENT" : season;
        List<SportsAuctionTeam> sorted = new ArrayList<>(teams);

        sorted.sort((t1, t2) -> {
            Integer rank1 = resolveTeamRank(t1, sportId, communityId, resolvedSeason);
            Integer rank2 = resolveTeamRank(t2, sportId, communityId, resolvedSeason);

            if (rank1 != null && rank2 != null) {
                return Integer.compare(rank1, rank2);
            }
            if (rank1 != null) return -1;
            if (rank2 != null) return 1;

            Integer rating1 = resolveTeamRating(t1, sportId, communityId, resolvedSeason);
            Integer rating2 = resolveTeamRating(t2, sportId, communityId, resolvedSeason);

            if (rating1 != null && rating2 != null) {
                return Integer.compare(rating2, rating1); // higher rating first
            }
            if (rating1 != null) return -1;
            if (rating2 != null) return 1;

            return Long.compare(t1.getId() != null ? t1.getId() : 0, t2.getId() != null ? t2.getId() : 0);
        });

        log.info("[SEEDING] Sorted {} teams for sportId={} communityId={} season={}", sorted.size(), sportId, communityId, resolvedSeason);
        return sorted;
    }

    /**
     * Backward-compatible overload without parameters.
     */
    public List<SportsAuctionTeam> seed(List<SportsAuctionTeam> teams) {
        return teams;
    }

    private Integer resolveTeamRank(SportsAuctionTeam team, Long sportId, Long communityId, String season) {
        Long userId = extractPrimaryUserId(team);
        if (userId == null) return null;

        return rankingRepo.findByUserIdAndSportIdAndCommunityIdAndSeason(userId, sportId, communityId, season)
                .map(SportsPlayerRanking::getRank)
                .orElseGet(() -> rankingRepo.findByUserIdAndSportIdAndCommunityId(userId, sportId, communityId)
                        .map(SportsPlayerRanking::getRank)
                        .orElse(null));
    }

    private Integer resolveTeamRating(SportsAuctionTeam team, Long sportId, Long communityId, String season) {
        Long userId = extractPrimaryUserId(team);
        if (userId == null) return null;

        return rankingRepo.findByUserIdAndSportIdAndCommunityIdAndSeason(userId, sportId, communityId, season)
                .map(SportsPlayerRanking::getRating)
                .orElseGet(() -> rankingRepo.findByUserIdAndSportIdAndCommunityId(userId, sportId, communityId)
                        .map(SportsPlayerRanking::getRating)
                        .orElse(null));
    }

    private Long extractPrimaryUserId(SportsAuctionTeam team) {
        if (team == null) return null;
        if (team.getCaptainUser() != null && team.getCaptainUser().getId() != null) {
            return team.getCaptainUser().getId();
        }
        if (team.getOwnerUser() != null && team.getOwnerUser().getId() != null) {
            return team.getOwnerUser().getId();
        }
        if (team.getPlayers() != null && !team.getPlayers().isEmpty()) {
            SportsAuctionPlayer first = team.getPlayers().get(0);
            if (first != null && first.getUser() != null) {
                return first.getUser().getId();
            }
        }
        return null;
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
