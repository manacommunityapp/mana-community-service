package com.manacommunity.api.sports.scheduler;
import com.manacommunity.api.sports.repository.*;
import com.manacommunity.api.sports.model.*;
import com.manacommunity.api.sports.dto.*;
import com.manacommunity.api.sports.model.MatchRound;
import com.manacommunity.api.sports.model.MatchStatus;


import com.manacommunity.api.security.AuditAction;

import com.manacommunity.api.security.AuditModule;

import com.manacommunity.api.security.AuditService;

import com.manacommunity.api.sports.dto.SportsMatchResultRequest;
import com.manacommunity.api.exception.ResourceNotFoundException;
import com.manacommunity.api.sports.model.SportsAuctionTeam;
import com.manacommunity.api.sports.model.SportsCourt;
import com.manacommunity.api.sports.repository.SportsAuctionTeamRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Generates match fixtures for every supported tournament format and advances
 * brackets as results come in. Time slots come from {@link SportsTimeSlotAllocator}
 * and courts are distributed round-robin via {@link SportsCourtAllocator}.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SportsBracketGenerator {

    private final SportsTournamentConfigRepository  configRepo;
    private final SportsTournamentGroupRepository   groupRepo;
    private final SportsTournamentMatchRepository   matchRepo;
    private final SportsGroupTeamStandingRepository standingRepo;
    private final SportsAuctionTeamRepository       teamRepo;
    private final SportsSeedingService              seeding;
    private final SportsTimeSlotAllocator           timeSlots;
    private final SportsCourtAllocator              courts;
    private final com.manacommunity.api.security.AuditService auditService;
    private final SportsTournamentRepository        tournamentRepo;

    /** Dispatch to the right generator for the config's tournament type. */
    public List<SportsTournamentMatch> generate(SportsTournamentConfig config, List<SportsAuctionTeam> teams) {
        return switch (config.getTournamentType()) {
            case KNOCKOUT, KNOCKOUT_SINGLE, CUSTOM   -> generateKnockout(config, teams);
            case GROUP_KNOCKOUT, GROUP_PLAYOFF        -> generateGroupKnockout(config, teams);
            case ROUND_ROBIN, LEAGUE                  -> generateRoundRobin(config, teams);
            case DOUBLE_ELIMINATION, KNOCKOUT_DOUBLE  -> generateDoubleElimination(config, teams);
            case SWISS                                -> generateSwissRound1(config, teams);
            case SUPER_LEAGUE                         -> generateSuperLeague(config, teams);
        };
    }

    // ═══════════════════════════════════════════════════════════════
    // 1. KNOCKOUT — single elimination bracket
    // ═══════════════════════════════════════════════════════════════
    private List<SportsTournamentMatch> generateKnockout(
            SportsTournamentConfig config, List<SportsAuctionTeam> teams) {

        int n         = teams.size();
        int rounds    = (int) Math.ceil(Math.log(n) / Math.log(2));  // ceil(log2(n))
        int slotCount = (int) Math.pow(2, rounds);                   // nearest power of 2
        int byes      = slotCount - n;

        List<SportsTournamentMatch> matches = new ArrayList<>();
        int matchDur  = Objects.requireNonNullElse(config.getMatchDurationMinutes(), 90);
        int breakMins = Objects.requireNonNullElse(config.getBreakBetweenMatchesMinutes(), 30);
        SportsTimeSlotAllocator.Cursor slots = timeSlots.cursor(config.getStartDate(), matchDur, breakMins);
        List<SportsCourt> venueCourts = courts.courtsFor(config);
        int courtIdx = 0;

        // ── Pad with nulls for byes ─────────────────────────────
        List<SportsAuctionTeam> slotTeams = new ArrayList<>(teams);
        for (int i = 0; i < byes; i++) slotTeams.add(null);  // null = bye

        // ── Round 1 ─────────────────────────────────────────────
        int matchNum = 1;

        for (int i = 0; i < slotCount; i += 2) {
            SportsAuctionTeam teamA = slotTeams.get(i);
            SportsAuctionTeam teamB = slotTeams.get(i + 1);

            boolean isBye          = (teamB == null);
            MatchStatus status     = isBye ? MatchStatus.BYE : MatchStatus.SCHEDULED;
            SportsAuctionTeam autoWinner = isBye ? teamA : null;

            // A bye isn't played, so it does not consume a time slot (the cursor is
            // not advanced for it) — real matches keep consecutive times.
            matches.add(SportsTournamentMatch.builder()
                .config(config)
                .community(config.getCommunity())
                .round(rounds == 1 ? MatchRound.FINAL : roundLabel(rounds, 1))
                .roundNumber(1)
                .matchNumber(matchNum++)
                .bracketSlot(i / 2)
                .teamA(teamA)
                .teamB(teamB)
                .scheduledAt(isBye ? slots.peek() : slots.next())
                .durationMinutes(matchDur)
                .venue(config.getVenue())
                .court(courts.pick(venueCourts, courtIdx++))
                .status(status)
                .winner(autoWinner)
                .build());
        }

        // ── Subsequent rounds (placeholder — filled by advanceBracket) ──
        int prevRoundSize = slotCount / 2;
        for (int round = 2; round <= rounds; round++) {
            int roundSize = prevRoundSize / 2;
            MatchRound label = roundLabel(rounds, round);

            for (int i = 0; i < roundSize; i++) {
                matches.add(SportsTournamentMatch.builder()
                    .config(config)
                    .community(config.getCommunity())
                    .round(label)
                    .roundNumber(round)
                    .matchNumber(i + 1)
                    .bracketSlot(i)
                    .teamA(null)   // TBD — winner of earlier match
                    .teamB(null)
                    .scheduledAt(slots.next())
                    .durationMinutes(matchDur)
                    .venue(config.getVenue())
                    .court(courts.pick(venueCourts, courtIdx++))
                    .status(MatchStatus.SCHEDULED)
                    .build());
            }

            // Third place match (same slot as final, different schedule)
            if (round == rounds && Boolean.TRUE.equals(config.getThirdPlaceMatch())) {
                matches.add(SportsTournamentMatch.builder()
                    .config(config)
                    .community(config.getCommunity())
                    .round(MatchRound.THIRD_PLACE)
                    .roundNumber(round)
                    .matchNumber(99)
                    .scheduledAt(slots.next())
                    .durationMinutes(matchDur)
                    .venue(config.getVenue())
                    .court(courts.pick(venueCourts, courtIdx++))
                    .status(MatchStatus.SCHEDULED)
                    .build());
            }
            prevRoundSize = roundSize;
        }

        // ── Wire winnerAdvancesToMatchId links ──────────────────
        wireKnockoutLinks(matches);
        log.info("[KNOCKOUT] {} rounds, {} slots, {} byes, {} matches",
            rounds, slotCount, byes, matches.size());
        return matches;
    }

    /** After a result is entered, advance winner/loser and update standings. Returns the config. */
    @Transactional
    public SportsTournamentConfig applyResult(SportsMatchResultRequest req) {
        SportsTournamentMatch match = matchRepo.findById(req.matchId())
            .orElseThrow(() -> new ResourceNotFoundException("SportsTournamentMatch", req.matchId()));

        match.setScoreTeamA(req.scoreTeamA());
        match.setScoreTeamB(req.scoreTeamB());
        match.setStatus(MatchStatus.COMPLETED);
        match.setCompletedAt(LocalDateTime.now());

        if (req.winnerTeamId() != null) {
            Optional<SportsAuctionTeam> winnerOpt = teamRepo.findById(req.winnerTeamId());
            if (winnerOpt.isPresent()) {
                SportsAuctionTeam winner = winnerOpt.get();
                SportsAuctionTeam loser = (match.getTeamA() != null && winner.getId().equals(match.getTeamA().getId()))
                    ? match.getTeamB() : match.getTeamA();

                match.setWinner(winner);
                match.setWinnerName(winner.getTeamName());
                match.setWinnerSide(match.getTeamA() != null && winner.getId().equals(match.getTeamA().getId()) ? "A" : "B");
                matchRepo.save(match);

                auditService.record(
                    com.manacommunity.api.security.AuditAction.WINNER_DECLARED,
                    com.manacommunity.api.security.AuditModule.TOURNAMENT,
                    "SportsTournamentMatch", String.valueOf(match.getId()),
                    null,
                    "winnerTeamId=" + winner.getId() + ", score=" + req.scoreTeamA() + "-" + req.scoreTeamB());

                // Advance winner to next round match
                if (match.getWinnerAdvancesToMatchId() != null) {
                    matchRepo.findById(match.getWinnerAdvancesToMatchId()).ifPresent(next -> {
                        if (next.getTeamA() == null) next.setTeamA(winner);
                        else next.setTeamB(winner);
                        advanceNonAuctionWinner(next, winner.getTeamName());
                        matchRepo.save(next);
                    });
                }

                // For double elimination: send loser to losers bracket
                if (match.getLoserSentToMatchId() != null && loser != null) {
                    matchRepo.findById(match.getLoserSentToMatchId()).ifPresent(losersMatch -> {
                        if (losersMatch.getTeamA() == null) losersMatch.setTeamA(loser);
                        else losersMatch.setTeamB(loser);
                        matchRepo.save(losersMatch);
                    });
                }

                // Update group standings for group matches
                if (match.getGroup() != null) {
                    updateGroupStandings(match, winner, loser, req);
                }
            } else {
                String winnerName = req.winnerName() != null ? req.winnerName() : "Winner #" + req.winnerTeamId();
                match.setWinnerName(winnerName);
                match.setWinnerSide(req.winnerSide());
                matchRepo.save(match);
                advanceWinnerToNextMatch(match, winnerName);
            }
        } else if (req.winnerName() != null && !req.winnerName().isBlank()) {
            match.setWinnerName(req.winnerName());
            match.setWinnerSide(req.winnerSide());
            matchRepo.save(match);

            auditService.record(
                com.manacommunity.api.security.AuditAction.WINNER_DECLARED,
                com.manacommunity.api.security.AuditModule.TOURNAMENT,
                "SportsTournamentMatch", String.valueOf(match.getId()),
                null,
                "winnerName=" + req.winnerName() + ", side=" + req.winnerSide() + ", score=" + req.scoreTeamA() + "-" + req.scoreTeamB());

            advanceWinnerToNextMatch(match, req.winnerName());
        } else {
            matchRepo.save(match);
            auditService.record(
                com.manacommunity.api.security.AuditAction.WINNER_DECLARED,
                com.manacommunity.api.security.AuditModule.TOURNAMENT,
                "SportsTournamentMatch", String.valueOf(match.getId()),
                null,
                "result=DRAW/TIE, score=" + req.scoreTeamA() + "-" + req.scoreTeamB());

            // Update group standings for draws
            if (match.getGroup() != null) {
                updateGroupStandingsForDraw(match, req);
            }
        }

        // When the tournament FINAL concludes, automatically mark the tournament and config as COMPLETED!
        if (match.getRound() == MatchRound.FINAL && match.getConfig() != null) {
            SportsTournamentConfig cfg = match.getConfig();
            cfg.setStatus(SportsTournamentConfig.TournamentStatus.COMPLETED);
            configRepo.save(cfg);
            if (cfg.getEvent() != null && cfg.getEvent().getTournament() != null) {
                SportsTournament t = cfg.getEvent().getTournament();
                t.setRegistrationStatus(SportsEventStatus.COMPLETED);
                tournamentRepo.save(t);
            }
        }

        return match.getConfig();
    }

    private void advanceWinnerToNextMatch(SportsTournamentMatch match, String winnerName) {
        if (match.getWinnerAdvancesToMatchId() != null && winnerName != null && !winnerName.isBlank()) {
            matchRepo.findById(match.getWinnerAdvancesToMatchId()).ifPresent(next -> {
                advanceNonAuctionWinner(next, winnerName);
                matchRepo.save(next);
            });
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // 2. GROUP_KNOCKOUT — groups + knockout stage
    // ═══════════════════════════════════════════════════════════════
    private List<SportsTournamentMatch> generateGroupKnockout(
            SportsTournamentConfig config, List<SportsAuctionTeam> teams) {

        int nGroups  = Objects.requireNonNullElse(config.getNumberOfGroups(), 2);
        int advPer   = Objects.requireNonNullElse(config.getTeamsAdvancingPerGroup(), 2);
        List<SportsTournamentMatch> matches = new ArrayList<>();
        LocalDateTime cursor = timeSlots.dayStart(config.getStartDate());
        int dur   = Objects.requireNonNullElse(config.getMatchDurationMinutes(), 90);
        int brk   = Objects.requireNonNullElse(config.getBreakBetweenMatchesMinutes(), 30);

        // Assign teams to groups (snake-draft for balanced seeding)
        // Team 1 → A, Team 2 → B, Team 3 → B, Team 4 → A …
        List<List<SportsAuctionTeam>> groupTeams = new ArrayList<>();
        for (int g = 0; g < nGroups; g++) groupTeams.add(new ArrayList<>());

        for (int i = 0; i < teams.size(); i++) {
            int row = i / nGroups;
            int col = (row % 2 == 0) ? (i % nGroups) : (nGroups - 1 - (i % nGroups));
            groupTeams.get(col).add(teams.get(i));
        }

        // ── Create group entities ────────────────────────────────
        char letter = 'A';
        for (int g = 0; g < nGroups; g++) {
            SportsTournamentGroup grp = SportsTournamentGroup.builder()
                .config(config)
                .groupName("Group " + (char)(letter + g))
                .groupOrder(g + 1)
                .build();
            grp = groupRepo.save(grp);

            // Standings
            List<SportsAuctionTeam> gt = groupTeams.get(g);
            for (int seed = 0; seed < gt.size(); seed++) {
                standingRepo.save(SportsGroupTeamStanding.builder()
                    .group(grp).team(gt.get(seed)).seedRank(seed + 1)
                    .played(0).won(0).lost(0).drawn(0).points(0).netRunRate(0.0)
                    .build());
            }

            // Group round-robin matches
            List<SportsTournamentMatch> groupMatches =
                roundRobinMatchesForGroup(grp, gt, config, cursor, dur, brk);
            matches.addAll(groupMatches);
            cursor = cursor.plusMinutes((long)(dur + brk) * groupMatches.size());
        }

        // ── Knockout placeholder matches ─────────────────────────
        // Total teams advancing = nGroups * advPer
        int advancing = nGroups * advPer;
        SportsTournamentConfig koConfig = config.toBuilder()
            .thirdPlaceMatch(config.getThirdPlaceMatch())
            .build();

        List<SportsTournamentMatch> koMatches = generateKnockout(koConfig,
            new ArrayList<>(Collections.nCopies(advancing, null))); // TBD teams
        // Adjust cursor for knockout
        for (int i = 0; i < koMatches.size(); i++) {
            koMatches.get(i).setScheduledAt(cursor.plusMinutes((long) (dur + brk) * i));
        }
        matches.addAll(koMatches);

        log.info("[GROUP_KNOCKOUT] {} groups, {} teams advancing per group, {} total matches",
            nGroups, advPer, matches.size());
        return matches;
    }

    /** Called when group stage is complete — seeds winners into knockout */
    @Transactional
    public void seedKnockoutFromGroups(Long configId) {
        SportsTournamentConfig config = configRepo.findById(configId)
            .orElseThrow(() -> new ResourceNotFoundException("SportsTournamentConfig", configId));
        int advPer = Objects.requireNonNullElse(config.getTeamsAdvancingPerGroup(), 2);

        List<SportsTournamentGroup> groups = groupRepo.findByConfigIdOrderByGroupOrder(configId);
        List<SportsAuctionTeam> advancingTeams = new ArrayList<>();

        // Collect top N from each group by points (then NRR)
        for (SportsTournamentGroup grp : groups) {
            List<SportsGroupTeamStanding> standings =
                standingRepo.findByGroupIdOrderByPointsDescNetRunRateDesc(grp.getId());
            standings.stream().limit(advPer)
                .forEach(s -> {
                    s.setQualified(true);
                    standingRepo.save(s);
                    if (s.getTeam() != null) {
                        advancingTeams.add(s.getTeam());
                    }
                });
        }

        // Seed into knockout round 1 matches (exclude group stages)
        List<SportsTournamentMatch> koBracket = matchRepo
            .findByConfigId(configId).stream()
            .filter(m -> m.getRound() != MatchRound.GROUP_STAGE && m.getRound() != MatchRound.LEAGUE_MATCH && m.getRound() != MatchRound.THIRD_PLACE)
            .sorted(Comparator.comparingInt(m -> Objects.requireNonNullElse(m.getRoundNumber(), 1)))
            .toList();

        // Standard cross seeding
        List<SportsAuctionTeam> seeded = seeding.crossSeed(advancingTeams, groups.size(), advPer);
        for (int i = 0; i < koBracket.size() && (i * 2 + 1) < seeded.size(); i++) {
            SportsTournamentMatch m = koBracket.get(i);
            m.setTeamA(seeded.get(i * 2));
            m.setTeamB(seeded.get(i * 2 + 1));
            matchRepo.save(m);
        }
        log.info("Seeded {} advancing teams into knockout bracket", advancingTeams.size());
    }

    // ═══════════════════════════════════════════════════════════════
    // 3. ROUND ROBIN — everyone vs everyone
    // ═══════════════════════════════════════════════════════════════
    private List<SportsTournamentMatch> generateRoundRobin(
            SportsTournamentConfig config, List<SportsAuctionTeam> teams) {

        List<SportsTournamentMatch> matches = new ArrayList<>();
        int n = teams.size();
        int dur = Objects.requireNonNullElse(config.getMatchDurationMinutes(), 90);
        int brk = Objects.requireNonNullElse(config.getBreakBetweenMatchesMinutes(), 30);
        SportsTimeSlotAllocator.Cursor slots = timeSlots.cursor(config.getStartDate(), dur, brk);
        List<SportsCourt> venueCourts = courts.courtsFor(config);
        int courtIdx = 0;
        int matchNum = 1;

        // ── Berger round-robin algorithm ────────────────────────
        // Fix team[0], rotate team[1..n-1] for each round
        List<SportsAuctionTeam> pool = new ArrayList<>(teams);
        if (n % 2 != 0) pool.add(null); // null = bye
        int totalSlots = pool.size();
        int rounds = totalSlots - 1;

        for (int round = 0; round < rounds; round++) {
            for (int i = 0; i < totalSlots / 2; i++) {
                SportsAuctionTeam teamA = pool.get(i);
                SportsAuctionTeam teamB = pool.get(totalSlots - 1 - i);

                if (teamA == null || teamB == null) continue; // bye round

                matches.add(SportsTournamentMatch.builder()
                    .config(config)
                    .community(config.getCommunity())
                    .round(MatchRound.LEAGUE_MATCH)
                    .roundNumber(round + 1)
                    .matchNumber(matchNum++)
                    .bracketSlot(i)
                    .teamA(teamA)
                    .teamB(teamB)
                    .scheduledAt(slots.next())
                    .durationMinutes(dur)
                    .venue(config.getVenue())
                    .court(courts.pick(venueCourts, courtIdx++))
                    .status(MatchStatus.SCHEDULED)
                    .build());
            }

            // Rotate: keep pool[0] fixed, rotate rest
            SportsAuctionTeam last = pool.remove(totalSlots - 1);
            pool.add(1, last);
        }

        log.info("[ROUND_ROBIN] {} teams, {} rounds, {} matches", n, rounds, matches.size());
        return matches;
    }

    // ═══════════════════════════════════════════════════════════════
    // 4. DOUBLE ELIMINATION — winners + losers bracket
    // ═══════════════════════════════════════════════════════════════
    private List<SportsTournamentMatch> generateDoubleElimination(
            SportsTournamentConfig config, List<SportsAuctionTeam> teams) {

        List<SportsTournamentMatch> matches = new ArrayList<>();
        int n         = teams.size();
        int rounds    = (int) Math.ceil(Math.log(n) / Math.log(2));
        int slotCount = (int) Math.pow(2, rounds);
        List<SportsAuctionTeam> slotTeams = new ArrayList<>(teams);
        while (slotTeams.size() < slotCount) slotTeams.add(null);

        int dur = Objects.requireNonNullElse(config.getMatchDurationMinutes(), 90);
        int brk = Objects.requireNonNullElse(config.getBreakBetweenMatchesMinutes(), 30);
        SportsTimeSlotAllocator.Cursor slots = timeSlots.cursor(config.getStartDate(), dur, brk);
        List<SportsCourt> venueCourts = courts.courtsFor(config);
        int courtIdx = 0;
        int matchNum = 1;

        // ── Winners Bracket ─────────────────────────────────────
        for (int round = 1; round <= rounds; round++) {
            int roundSize = slotCount / (int) Math.pow(2, round);
            for (int i = 0; i < roundSize; i++) {
                matches.add(SportsTournamentMatch.builder()
                    .config(config)
                    .community(config.getCommunity())
                    .round(MatchRound.WINNERS_BRACKET)
                    .roundNumber(round)
                    .matchNumber(matchNum++)
                    .bracketSlot(i)
                    .teamA(round == 1 ? slotTeams.get(i * 2) : null)
                    .teamB(round == 1 ? slotTeams.get(i * 2 + 1) : null)
                    .scheduledAt(slots.next())
                    .durationMinutes(dur)
                    .venue(config.getVenue())
                    .court(courts.pick(venueCourts, courtIdx++))
                    .status(MatchStatus.SCHEDULED)
                    .build());
            }
        }

        // ── Losers Bracket ──────────────────────────────────────
        int lbRounds = 2 * (rounds - 1);
        for (int round = 1; round <= lbRounds; round++) {
            int lbSize = (int)(slotCount / Math.pow(2, Math.ceil(round / 2.0) + 1));
            lbSize = Math.max(1, lbSize);
            for (int i = 0; i < lbSize; i++) {
                matches.add(SportsTournamentMatch.builder()
                    .config(config)
                    .community(config.getCommunity())
                    .round(MatchRound.LOSERS_BRACKET)
                    .roundNumber(round)
                    .matchNumber(matchNum++)
                    .bracketSlot(i)
                    .teamA(null).teamB(null)  // filled by advance logic
                    .scheduledAt(slots.next())
                    .durationMinutes(dur)
                    .venue(config.getVenue())
                    .court(courts.pick(venueCourts, courtIdx++))
                    .status(MatchStatus.SCHEDULED)
                    .build());
            }
        }

        // ── Grand Final ─────────────────────────────────────────
        matches.add(SportsTournamentMatch.builder()
            .config(config).community(config.getCommunity())
            .round(MatchRound.GRAND_FINAL).roundNumber(rounds + lbRounds + 1)
            .matchNumber(matchNum++)
            .scheduledAt(slots.next()).durationMinutes(dur).venue(config.getVenue())
            .court(courts.pick(venueCourts, courtIdx++))
            .status(MatchStatus.SCHEDULED).build());

        log.info("[DOUBLE_ELIM] {} rounds WB, {} rounds LB, {} total matches",
            rounds, lbRounds, matches.size());
        return matches;
    }

    // ═══════════════════════════════════════════════════════════════
    // 5. SWISS SYSTEM — score-paired rounds
    // ═══════════════════════════════════════════════════════════════
    private List<SportsTournamentMatch> generateSwissRound1(
            SportsTournamentConfig config, List<SportsAuctionTeam> teams) {

        int rounds = Objects.requireNonNullElse(
            config.getSwissRounds(),
            (int) Math.ceil(Math.log(teams.size()) / Math.log(2)));

        List<SportsTournamentMatch> matches = new ArrayList<>();
        LocalDateTime cursor = timeSlots.dayStart(config.getStartDate());
        int dur = Objects.requireNonNullElse(config.getMatchDurationMinutes(), 90);
        int brk = Objects.requireNonNullElse(config.getBreakBetweenMatchesMinutes(), 30);

        // Round 1: random pairing
        List<SportsAuctionTeam> shuffled = new ArrayList<>(teams);
        Collections.shuffle(shuffled);
        List<SportsTournamentMatch> round1 = pairSwissRound(config, shuffled, 1, cursor, dur, brk);
        matches.addAll(round1);

        log.info("[SWISS] {} rounds planned, round 1 generated ({} matches)",
            rounds, round1.size());
        return matches;
    }

    /** Called after each Swiss round result to generate next round pairing */
    @Transactional
    public List<SportsTournamentMatch> generateNextSwissRound(Long configId) {
        SportsTournamentConfig config = configRepo.findById(configId)
            .orElseThrow(() -> new ResourceNotFoundException("SportsTournamentConfig", configId));

        int lastRound = matchRepo.findMaxSwissRound(configId).orElse(0);
        int nextRound = lastRound + 1;
        int maxRounds = Objects.requireNonNullElse(config.getSwissRounds(), 5);

        if (nextRound > maxRounds)
            throw new com.manacommunity.api.exception.AuctionStateException("Swiss tournament already completed " + maxRounds + " rounds");

        // Build score map: teamId → points
        Map<Long, Integer> scores = new HashMap<>();
        matchRepo.findByConfigIdOrderBySwissRoundNumber(configId).stream()
            .filter(m -> m.getStatus() == MatchStatus.COMPLETED)
            .forEach(m -> {
                if (m.getWinner() != null && m.getTeamA() != null && m.getTeamB() != null) {
                    Long wId = m.getWinner().getId();
                    Long lId = wId.equals(m.getTeamA().getId())
                        ? m.getTeamB().getId() : m.getTeamA().getId();
                    scores.merge(wId, config.getPointsForWin() != null ? config.getPointsForWin() : 2, Integer::sum);
                    scores.merge(lId, config.getPointsForLoss() != null ? config.getPointsForLoss() : 0, Integer::sum);
                } else if (m.getTeamA() != null && m.getTeamB() != null) {
                    int drawPts = config.getPointsForDraw() != null ? config.getPointsForDraw() : 1;
                    scores.merge(m.getTeamA().getId(), drawPts, Integer::sum);
                    scores.merge(m.getTeamB().getId(), drawPts, Integer::sum);
                }
            });

        Set<Long> teamIdsInConfig = new HashSet<>();
        matchRepo.findByConfigId(configId).forEach(m -> {
            if (m.getTeamA() != null) teamIdsInConfig.add(m.getTeamA().getId());
            if (m.getTeamB() != null) teamIdsInConfig.add(m.getTeamB().getId());
        });
        List<SportsAuctionTeam> tournamentTeams = teamRepo.findAllById(teamIdsInConfig);

        tournamentTeams.sort((a, b) -> {
            int sa = scores.getOrDefault(a.getId(), 0);
            int sb = scores.getOrDefault(b.getId(), 0);
            if (sa != sb) return sb - sa;
            return (int)(Math.random() * 3 - 1); // random tiebreak
        });

        // Build set of already-played pairs to avoid rematches
        Set<String> playedPairs = matchRepo.findByConfigId(configId).stream()
            .filter(m -> m.getTeamA() != null && m.getTeamB() != null)
            .map(m -> Math.min(m.getTeamA().getId(), m.getTeamB().getId()) + "-"
                    + Math.max(m.getTeamA().getId(), m.getTeamB().getId()))
            .collect(Collectors.toSet());

        // Pair adjacent teams by score, avoid rematches
        List<SportsAuctionTeam> paired = swissPairNoRematches(tournamentTeams, playedPairs);
        LocalDateTime cursor = LocalDateTime.now().plusDays(1).withHour(9).withMinute(0);
        int dur = Objects.requireNonNullElse(config.getMatchDurationMinutes(), 90);
        int brk = Objects.requireNonNullElse(config.getBreakBetweenMatchesMinutes(), 30);

        List<SportsTournamentMatch> newMatches = pairSwissRound(config, paired, nextRound, cursor, dur, brk);
        matchRepo.saveAll(newMatches);
        log.info("[SWISS] Round {} generated ({} matches)", nextRound, newMatches.size());
        return newMatches;
    }

    // ═══════════════════════════════════════════════════════════════
    // 6. SUPER LEAGUE — IPL-style top-4 playoffs
    // ═══════════════════════════════════════════════════════════════
    private List<SportsTournamentMatch> generateSuperLeague(
            SportsTournamentConfig config, List<SportsAuctionTeam> teams) {

        // Phase 1: Full round-robin league stage (everyone vs everyone)
        List<SportsTournamentMatch> leagueMatches = generateRoundRobin(config, teams);

        // Phase 2: Playoffs for top 4
        int dur = Objects.requireNonNullElse(config.getMatchDurationMinutes(), 90);
        LocalDateTime playoffStart = config.getStartDate()
            .plusDays((long) leagueMatches.size() / 2 + 1).atTime(19, 0);
        List<SportsCourt> venueCourts = courts.courtsFor(config);

        List<SportsTournamentMatch> playoffs = List.of(
            buildPlayoffMatch(config, MatchRound.QUALIFIER_1,  1, playoffStart,             dur, venueCourts, 0),
            buildPlayoffMatch(config, MatchRound.ELIMINATOR,   2, playoffStart.plusDays(1), dur, venueCourts, 1),
            buildPlayoffMatch(config, MatchRound.QUALIFIER_2,  3, playoffStart.plusDays(3), dur, venueCourts, 2),
            buildPlayoffMatch(config, MatchRound.SUPER_FINAL,  4, playoffStart.plusDays(5), dur, venueCourts, 3)
        );

        List<SportsTournamentMatch> all = new ArrayList<>(leagueMatches);
        all.addAll(playoffs);
        log.info("[SUPER_LEAGUE] {} league + {} playoff = {} total",
            leagueMatches.size(), playoffs.size(), all.size());
        return all;
    }

    // ═══════════════════════════════════════════════════════════════
    // STANDINGS UPDATE
    // ═══════════════════════════════════════════════════════════════
    private SportsGroupTeamStanding getOrCreateStanding(SportsTournamentGroup group, SportsAuctionTeam team) {
        if (group == null || team == null || team.getId() == null) return null;
        SportsGroupTeamStanding s = standingRepo.findByGroupIdAndTeamId(group.getId(), team.getId());
        if (s == null) {
            s = SportsGroupTeamStanding.builder()
                .group(group)
                .team(team)
                .seedRank(0)
                .played(0).won(0).lost(0).drawn(0).points(0).netRunRate(0.0)
                .build();
            s = standingRepo.save(s);
        }
        return s;
    }

    private void updateGroupStandings(SportsTournamentMatch match,
            SportsAuctionTeam winner, SportsAuctionTeam loser, SportsMatchResultRequest req) {
        if (match == null || match.getGroup() == null) return;

        int ptsWin  = Objects.requireNonNullElse(match.getConfig().getPointsForWin(), 2);
        int ptsLoss = Objects.requireNonNullElse(match.getConfig().getPointsForLoss(), 0);

        // Update winner
        if (winner != null) {
            SportsGroupTeamStanding ws = getOrCreateStanding(match.getGroup(), winner);
            if (ws != null) {
                ws.setPlayed(ws.getPlayed() + 1);
                ws.setWon(ws.getWon() + 1);
                ws.setPoints(ws.getPoints() + ptsWin);
                if (req.runsTeamA() != null) updateNRR(ws, winner, match, req);
                standingRepo.save(ws);
            }
        }

        // Update loser
        if (loser != null) {
            SportsGroupTeamStanding ls = getOrCreateStanding(match.getGroup(), loser);
            if (ls != null) {
                ls.setPlayed(ls.getPlayed() + 1);
                ls.setLost(ls.getLost() + 1);
                ls.setPoints(ls.getPoints() + ptsLoss);
                if (req.runsTeamB() != null) updateNRR(ls, loser, match, req);
                standingRepo.save(ls);
            }
        }
    }

    private void updateNRR(SportsGroupTeamStanding standing, SportsAuctionTeam team,
            SportsTournamentMatch match, SportsMatchResultRequest req) {
        if (standing == null || team == null || match == null) return;
        boolean isTeamA = match.getTeamA() != null && team.getId().equals(match.getTeamA().getId());
        int runsFor     = isTeamA ? (req.runsTeamA() != null ? req.runsTeamA() : 0) : (req.runsTeamB() != null ? req.runsTeamB() : 0);
        int runsAgainst = isTeamA ? (req.runsTeamB() != null ? req.runsTeamB() : 0) : (req.runsTeamA() != null ? req.runsTeamA() : 0);
        int oversFor    = isTeamA ? (req.oversTeamA() != null ? req.oversTeamA() : 0) : (req.oversTeamB() != null ? req.oversTeamB() : 0);
        int oversAgainst= isTeamA ? (req.oversTeamB() != null ? req.oversTeamB() : 0) : (req.oversTeamA() != null ? req.oversTeamA() : 0);

        standing.setRunsFor(Objects.requireNonNullElse(standing.getRunsFor(), 0) + runsFor);
        standing.setRunsAgainst(Objects.requireNonNullElse(standing.getRunsAgainst(), 0) + runsAgainst);
        standing.setOversFor(Objects.requireNonNullElse(standing.getOversFor(), 0) + oversFor);
        standing.setOversAgainst(Objects.requireNonNullElse(standing.getOversAgainst(), 0) + oversAgainst);

        double nrr = (standing.getOversFor() > 0 ? (double) standing.getRunsFor() / standing.getOversFor() : 0)
                   - (standing.getOversAgainst() > 0 ? (double) standing.getRunsAgainst() / standing.getOversAgainst() : 0);
        standing.setNetRunRate(Math.round(nrr * 1000.0) / 1000.0);
    }

    private void updateGroupStandingsForDraw(SportsTournamentMatch match, SportsMatchResultRequest req) {
        if (match == null || match.getGroup() == null) return;
        int ptsDraw = Objects.requireNonNullElse(match.getConfig().getPointsForDraw(), 1);

        if (match.getTeamA() != null) {
            SportsGroupTeamStanding sa = getOrCreateStanding(match.getGroup(), match.getTeamA());
            if (sa != null) {
                sa.setPlayed(sa.getPlayed() + 1);
                sa.setDrawn(sa.getDrawn() + 1);
                sa.setPoints(sa.getPoints() + ptsDraw);
                if (req.runsTeamA() != null) updateNRR(sa, match.getTeamA(), match, req);
                standingRepo.save(sa);
            }
        }

        if (match.getTeamB() != null) {
            SportsGroupTeamStanding sb = getOrCreateStanding(match.getGroup(), match.getTeamB());
            if (sb != null) {
                sb.setPlayed(sb.getPlayed() + 1);
                sb.setDrawn(sb.getDrawn() + 1);
                sb.setPoints(sb.getPoints() + ptsDraw);
                if (req.runsTeamB() != null) updateNRR(sb, match.getTeamB(), match, req);
                standingRepo.save(sb);
            }
        }
    }

    private void advanceNonAuctionWinner(SportsTournamentMatch next, String winnerName) {
        if (next == null || winnerName == null || winnerName.isBlank()) return;
        String notes = next.getMatchNotes();
        String home = extractNote(notes, "homeName", "");
        String away = extractNote(notes, "awayName", "");
        String group = extractNote(notes, "groupName", "");
        if (home.isBlank() || "TBD".equalsIgnoreCase(home) || "dummy".equalsIgnoreCase(home)) {
            home = winnerName;
        } else if (away.isBlank() || "TBD".equalsIgnoreCase(away) || "dummy".equalsIgnoreCase(away)) {
            away = winnerName;
        }
        next.setMatchNotes(String.format("{\"homeName\":\"%s\",\"awayName\":\"%s\",\"groupName\":\"%s\"}",
            home.replace("\"", "'"), away.replace("\"", "'"), group.replace("\"", "'")));
    }

    private String extractNote(String notes, String key, String fallback) {
        if (notes == null || notes.isBlank()) return fallback;
        try {
            String pattern = "\"" + key + "\":\"([^\"]*)\"";
            java.util.regex.Matcher matcher = java.util.regex.Pattern.compile(pattern).matcher(notes);
            if (matcher.find()) {
                String val = matcher.group(1);
                if (val != null && !val.isBlank() && !"dummy".equalsIgnoreCase(val)) {
                    return val;
                }
            }
        } catch (Exception ignored) {}
        return fallback;
    }

    // ═══════════════════════════════════════════════════════════════
    // HELPERS
    // ═══════════════════════════════════════════════════════════════
    private MatchRound roundLabel(int totalRounds, int round) {
        int fromEnd = totalRounds - round;
        return switch (fromEnd) {
            case 0 -> MatchRound.FINAL;
            case 1 -> MatchRound.SEMI_FINAL;
            case 2 -> MatchRound.QUARTER_FINAL;
            case 3 -> MatchRound.ROUND_OF_16;
            case 4 -> MatchRound.ROUND_OF_32;
            default -> MatchRound.ROUND_OF_64;
        };
    }

    private List<SportsTournamentMatch> roundRobinMatchesForGroup(
            SportsTournamentGroup grp, List<SportsAuctionTeam> teams,
            SportsTournamentConfig config, LocalDateTime start, int dur, int brk) {

        List<SportsTournamentMatch> matches = new ArrayList<>();
        int matchNum = 1;
        SportsTimeSlotAllocator.Cursor slots = timeSlots.cursor(start, dur, brk);
        List<SportsCourt> venueCourts = courts.courtsFor(config);
        int courtIdx = 0;

        for (int i = 0; i < teams.size(); i++) {
            for (int j = i + 1; j < teams.size(); j++) {
                matches.add(SportsTournamentMatch.builder()
                    .config(config).community(config.getCommunity()).group(grp)
                    .round(MatchRound.GROUP_STAGE).roundNumber(matchNum).matchNumber(matchNum)
                    .teamA(teams.get(i)).teamB(teams.get(j))
                    .scheduledAt(slots.next()).durationMinutes(dur).venue(config.getVenue())
                    .court(courts.pick(venueCourts, courtIdx++))
                    .status(MatchStatus.SCHEDULED).build());
                matchNum++;
            }
        }
        return matches;
    }

    private List<SportsTournamentMatch> pairSwissRound(
            SportsTournamentConfig config, List<SportsAuctionTeam> teams,
            int roundNum, LocalDateTime start, int dur, int brk) {

        List<SportsTournamentMatch> matches = new ArrayList<>();
        SportsTimeSlotAllocator.Cursor slots = timeSlots.cursor(start, dur, brk);
        List<SportsCourt> venueCourts = courts.courtsFor(config);
        int courtIdx = 0;
        for (int i = 0; i + 1 < teams.size(); i += 2) {
            matches.add(SportsTournamentMatch.builder()
                .config(config).community(config.getCommunity()).round(MatchRound.SWISS_ROUND)
                .roundNumber(roundNum).swissRoundNumber(roundNum)
                .matchNumber(i / 2 + 1).bracketSlot(i / 2)
                .teamA(teams.get(i)).teamB(teams.get(i + 1))
                .scheduledAt(slots.next()).durationMinutes(dur).venue(config.getVenue())
                .court(courts.pick(venueCourts, courtIdx++))
                .status(MatchStatus.SCHEDULED).build());
        }
        return matches;
    }

    private List<SportsAuctionTeam> swissPairNoRematches(
            List<SportsAuctionTeam> sorted, Set<String> playedPairs) {

        List<SportsAuctionTeam> result = new ArrayList<>();
        boolean[] used = new boolean[sorted.size()];

        for (int i = 0; i < sorted.size(); i++) {
            if (used[i]) continue;
            for (int j = i + 1; j < sorted.size(); j++) {
                if (used[j]) continue;
                String key = Math.min(sorted.get(i).getId(), sorted.get(j).getId()) + "-"
                           + Math.max(sorted.get(i).getId(), sorted.get(j).getId());
                if (!playedPairs.contains(key)) {
                    result.add(sorted.get(i));
                    result.add(sorted.get(j));
                    used[i] = used[j] = true;
                    break;
                }
            }
        }
        return result;
    }

    private SportsTournamentMatch buildPlayoffMatch(
            SportsTournamentConfig config, MatchRound round,
            int roundNum, LocalDateTime at, int dur,
            List<SportsCourt> venueCourts, int courtIndex) {
        return SportsTournamentMatch.builder()
            .config(config).community(config.getCommunity()).round(round).roundNumber(roundNum).matchNumber(1)
            .scheduledAt(at).durationMinutes(dur).venue(config.getVenue())
            .court(courts.pick(venueCourts, courtIndex))
            .status(MatchStatus.SCHEDULED).build();
    }

    private void wireKnockoutLinks(List<SportsTournamentMatch> matches) {
        // Group by roundNumber
        Map<Integer, List<SportsTournamentMatch>> byRound = matches.stream()
            .collect(Collectors.groupingBy(SportsTournamentMatch::getRoundNumber));

        byRound.forEach((round, rMatches) -> {
            List<SportsTournamentMatch> nextRound = byRound.get(round + 1);
            if (nextRound == null) return;
            for (int i = 0; i < rMatches.size(); i++) {
                SportsTournamentMatch current = rMatches.get(i);
                SportsTournamentMatch next    = nextRound.get(i / 2);
                current.setWinnerFeedFromMatchA(
                    (i % 2 == 0) ? current.getId() : current.getWinnerFeedFromMatchA());
                // Links wired after DB save so IDs are available
            }
        });
    }
}
