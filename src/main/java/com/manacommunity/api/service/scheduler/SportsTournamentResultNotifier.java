package com.manacommunity.api.service.scheduler;

import com.manacommunity.api.email.SportsTournamentResultEmailService;
import com.manacommunity.api.user.model.AppUser;
import com.manacommunity.api.model.SportsAuctionPlayer;
import com.manacommunity.api.model.SportsAuctionTeam;
import com.manacommunity.api.model.SportsEventRegistration;
import com.manacommunity.api.model.scheduler.MatchRound;
import com.manacommunity.api.model.scheduler.MatchStatus;
import com.manacommunity.api.model.scheduler.SportsTournamentConfig;
import com.manacommunity.api.model.scheduler.SportsTournamentMatch;
import com.manacommunity.api.repository.SportsEventRegistrationRepository;
import com.manacommunity.api.repository.scheduler.SportsTournamentMatchRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Fires the result-stage emails after a match result is applied:
 * <ul>
 *   <li><b>Winner Notification</b> to the winning side's players, for every completed match;</li>
 *   <li><b>SportsTournament Completion</b> to all confirmed participants when the FINAL is decided
 *       (champion = final winner, runner-up = final loser, third place from the 3rd-place match).</li>
 * </ul>
 * Read-only and self-contained; the underlying email service swallows its own failures.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SportsTournamentResultNotifier {

    private final SportsTournamentMatchRepository         matchRepo;
    private final SportsEventRegistrationRepository registrationRepo;
    private final SportsTournamentResultEmailService      resultEmailService;

    @Transactional(readOnly = true)
    public void notifyMatchResult(Long matchId) {
        SportsTournamentMatch match = matchRepo.findById(matchId).orElse(null);
        if (match == null || match.getWinner() == null) return;

        SportsTournamentConfig config = match.getConfig();
        String tournamentName = config != null ? config.getTournamentName() : "the tournament";
        String roundName = match.getRound() != null ? prettify(match.getRound().name()) : "your match";

        SportsAuctionTeam winnerTeam = match.getWinner();
        SportsAuctionTeam loserTeam = otherSide(match, winnerTeam);
        String opponentName = loserTeam != null ? loserTeam.getTeamName() : "your opponent";
        String score = score(match);

        // 1. Winner notification → the winning side's players.
        for (AppUser user : teamUsers(winnerTeam)) {
            resultEmailService.sendWinnerNotification(user, tournamentName, roundName, opponentName, score, null);
        }

        // 2. SportsTournament completion → everyone, once the FINAL is decided.
        if (match.getRound() == MatchRound.FINAL && config != null) {
            String champion = winnerTeam.getTeamName();
            String runnerUp = loserTeam != null ? loserTeam.getTeamName() : "";
            String thirdPlace = thirdPlaceName(config.getId());
            resultEmailService.sendTournamentCompletion(
                    confirmedParticipants(config), tournamentName, champion, runnerUp, thirdPlace);
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────

    private SportsAuctionTeam otherSide(SportsTournamentMatch m, SportsAuctionTeam winner) {
        if (m.getTeamA() != null && winner.getId().equals(m.getTeamA().getId())) return m.getTeamB();
        return m.getTeamA();
    }

    private String score(SportsTournamentMatch m) {
        String a = m.getScoreTeamA();
        String b = m.getScoreTeamB();
        if ((a == null || a.isBlank()) && (b == null || b.isBlank())) return "";
        return (a == null ? "" : a) + " - " + (b == null ? "" : b);
    }

    private String thirdPlaceName(Long configId) {
        return matchRepo.findByConfigId(configId).stream()
                .filter(m -> m.getRound() == MatchRound.THIRD_PLACE
                        && m.getStatus() == MatchStatus.COMPLETED && m.getWinner() != null)
                .map(m -> m.getWinner().getTeamName())
                .findFirst()
                .orElse("");
    }

    private List<AppUser> confirmedParticipants(SportsTournamentConfig config) {
        if (config.getEvent() == null) return List.of();
        return registrationRepo
                .findByEventIdAndStatus(config.getEvent().getId(),
                        SportsEventRegistration.RegistrationStatus.CONFIRMED)
                .stream()
                .map(SportsEventRegistration::getUser)
                .filter(Objects::nonNull)
                .toList();
    }

    /** Distinct, email-bearing users of a team: roster players plus owner and captain. */
    private List<AppUser> teamUsers(SportsAuctionTeam team) {
        if (team == null) return List.of();
        Map<Long, AppUser> byId = new LinkedHashMap<>();
        add(byId, team.getOwnerUser());
        add(byId, team.getCaptainUser());
        if (team.getPlayers() != null) {
            for (SportsAuctionPlayer p : team.getPlayers()) {
                if (p != null) add(byId, p.getUser());
            }
        }
        return new ArrayList<>(byId.values());
    }

    private void add(Map<Long, AppUser> byId, AppUser user) {
        if (user == null || user.getId() == null) return;
        if (user.getEmail() == null || user.getEmail().isBlank()) return;
        byId.putIfAbsent(user.getId(), user);
    }

    /** "SEMI_FINAL" -> "Semi Final". */
    private String prettify(String enumName) {
        if (enumName == null || enumName.isBlank()) return "";
        String[] parts = enumName.toLowerCase().split("_");
        StringBuilder sb = new StringBuilder();
        for (String p : parts) {
            if (p.isEmpty()) continue;
            sb.append(Character.toUpperCase(p.charAt(0))).append(p.substring(1)).append(' ');
        }
        return sb.toString().trim();
    }
}
