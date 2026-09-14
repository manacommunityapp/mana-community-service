package com.manacommunity.api.service.scheduler;

import com.manacommunity.api.dto.scheduler.SportsGenericLeaderboardEntry;
import com.manacommunity.api.model.scheduler.SportsMatchEvent;
import com.manacommunity.api.model.scheduler.SportsTournamentConfig;
import com.manacommunity.api.model.scheduler.SportsTournamentMatch;
import com.manacommunity.api.repository.scheduler.SportsMatchEventRepository;
import com.manacommunity.api.repository.scheduler.SportsTournamentConfigRepository;
import com.manacommunity.api.repository.scheduler.SportsTournamentMatchRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SportsGenericLeaderboardService {

    private final SportsMatchEventRepository eventRepo;
    private final SportsTournamentMatchRepository matchRepo;
    private final SportsTournamentConfigRepository configRepo;

    private static final Map<String, List<String>> SPORT_CATEGORIES = Map.of(
        "FOOTBALL", List.of("goals", "assists", "cards"),
        "SOCCER", List.of("goals", "assists", "cards"),
        "BASKETBALL", List.of("points", "three_pointers", "rebounds"),
        "BADMINTON", List.of("points", "aces"),
        "VOLLEYBALL", List.of("points", "aces", "blocks"),
        "TABLE_TENNIS", List.of("points", "aces"),
        "TENNIS", List.of("points", "aces")
    );

    private static final Map<String, Set<String>> CATEGORY_EVENT_TYPES = Map.of(
        "goals", Set.of("GOAL"),
        "assists", Set.of("ASSIST"),
        "cards", Set.of("YELLOW_CARD", "RED_CARD"),
        "points", Set.of("POINT", "GOAL", "TWO_POINTER", "THREE_POINTER", "FREE_THROW"),
        "three_pointers", Set.of("THREE_POINTER"),
        "rebounds", Set.of("REBOUND"),
        "aces", Set.of("ACE"),
        "blocks", Set.of("BLOCK")
    );

    @Transactional(readOnly = true)
    public List<SportsGenericLeaderboardEntry> getLeaderboard(Long configId, String category) {
        List<SportsTournamentMatch> matches = matchRepo.findByConfigId(configId);
        if (matches.isEmpty()) return List.of();

        List<Long> matchIds = matches.stream().map(SportsTournamentMatch::getId).toList();
        List<SportsMatchEvent> events = eventRepo.findActiveEventsByMatchIds(matchIds);

        Set<String> targetEventTypes = CATEGORY_EVENT_TYPES.getOrDefault(category, Set.of());
        boolean usePointsSum = Set.of("points", "three_pointers").contains(category);

        Map<Long, PlayerAgg> playerMap = new LinkedHashMap<>();

        for (SportsMatchEvent event : events) {
            if (event.getPlayer() == null) continue;
            if (!targetEventTypes.contains(event.getEventType())) continue;

            Long playerId = event.getPlayer().getId();
            playerMap.computeIfAbsent(playerId, k -> new PlayerAgg(
                playerId,
                event.getPlayer().getPlayerName(),
                event.getTeam().getId(),
                event.getTeam().getTeamName()
            ));

            PlayerAgg agg = playerMap.get(playerId);
            agg.matchIds.add(event.getMatch().getId());
            if (usePointsSum) {
                agg.value += (event.getPointsAwarded() != null ? event.getPointsAwarded() : 0);
            } else {
                agg.value++;
            }
        }

        List<PlayerAgg> sorted = playerMap.values().stream()
            .sorted(Comparator.comparingInt((PlayerAgg a) -> a.value).reversed())
            .toList();

        List<SportsGenericLeaderboardEntry> entries = new ArrayList<>();
        for (int i = 0; i < sorted.size(); i++) {
            PlayerAgg agg = sorted.get(i);
            entries.add(new SportsGenericLeaderboardEntry(
                agg.playerId, agg.playerName, agg.teamId, agg.teamName,
                category, agg.value, agg.matchIds.size(), i + 1
            ));
        }
        return entries;
    }

    public List<String> getCategories(Long configId) {
        SportsTournamentConfig config = configRepo.findById(configId).orElse(null);
        if (config == null || config.getSport() == null) return List.of("points");
        String sportName = config.getSport().getName().toUpperCase().replace(" ", "_").replace("-", "_");
        return SPORT_CATEGORIES.getOrDefault(sportName, List.of("points"));
    }

    public String resolveSportType(Long configId) {
        SportsTournamentConfig config = configRepo.findById(configId).orElse(null);
        if (config == null || config.getSport() == null) return "UNKNOWN";
        return config.getSport().getName().toUpperCase().replace(" ", "_").replace("-", "_");
    }

    private static class PlayerAgg {
        Long playerId;
        String playerName;
        Long teamId;
        String teamName;
        int value;
        Set<Long> matchIds = new HashSet<>();

        PlayerAgg(Long playerId, String playerName, Long teamId, String teamName) {
            this.playerId = playerId;
            this.playerName = playerName;
            this.teamId = teamId;
            this.teamName = teamName;
        }
    }
}
