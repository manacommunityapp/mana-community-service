package com.manacommunity.api.sports.scheduler.engine;

import com.manacommunity.api.sports.model.SportsCourt;
import com.manacommunity.api.sports.scheduler.engine.constraint.*;
import com.manacommunity.api.sports.scheduler.engine.dto.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class SportsSchedulerConstraintEngine {

    private final FlatConflictConstraint flatConstraint;
    private final TowerDispersionConstraint towerConstraint;
    private final FairByeAllocationConstraint byeConstraint;
    private final BalancedBracketConstraint bracketConstraint;
    private final RestPeriodConstraint restConstraint;
    private final CourtAllocationConstraint courtConstraint;
    private final TimeSlotAllocationConstraint timeSlotConstraint;
    private final RatingBalancingConstraint ratingConstraint;

    /**
     * Master generation method enforcing all 8 authoritative constraints.
     */
    public List<ScheduledMatchNode> generateAuthoritativeSchedule(
            List<ParticipantNode> rawParticipants,
            List<SportsCourt> availableCourts,
            ScheduleConstraintOptions options) {

        log.info("Generating schedule for {} participants with {} courts",
                rawParticipants.size(), availableCourts.size());

        // 1. Rating Balancing & Seeding (Rule 8)
        List<ParticipantNode> seededParticipants = options.isEnforceRatingBalancing()
                ? ratingConstraint.balanceAndSeed(rawParticipants)
                : new ArrayList<>(rawParticipants);

        // 2. Fair BYE Allocation (Rule 3)
        int targetBracketSize = byeConstraint.nextPowerOfTwo(seededParticipants.size());
        int byesNeeded = byeConstraint.calculateByes(seededParticipants.size());
        List<ParticipantNode> bracketNodes = byeConstraint.padWithByes(seededParticipants);

        // 3. Balanced Bracket Structure (Rule 4)
        List<Integer> seedOrder = bracketConstraint.generateStandardSeedPositions(targetBracketSize);
        ParticipantNode[] orderedDraw = new ParticipantNode[targetBracketSize];

        Map<Integer, ParticipantNode> seedMap = new HashMap<>();
        List<ParticipantNode> unseeded = new ArrayList<>();
        for (ParticipantNode p : bracketNodes) {
            if (p.getSeed() != null && p.getSeed() <= targetBracketSize) {
                seedMap.put(p.getSeed(), p);
            } else {
                unseeded.add(p);
            }
        }

        for (int i = 0; i < targetBracketSize; i++) {
            int targetSeed = seedOrder.get(i);
            if (seedMap.containsKey(targetSeed)) {
                orderedDraw[i] = seedMap.get(targetSeed);
            } else if (!unseeded.isEmpty()) {
                orderedDraw[i] = unseeded.remove(0);
            }
        }

        // 4. Resolve Flat (Rule 1) and Tower (Rule 2) Conflicts in Round 1
        if (options.isEnforceFlatSeparation() || options.isEnforceTowerDispersion()) {
            resolveFirstRoundConflicts(orderedDraw, options);
        }

        // 5. Generate Match Tree Nodes with Courts (Rule 6) & Time Slots (Rule 7) & Rest (Rule 5)
        return buildScheduledRounds(orderedDraw, targetBracketSize, availableCourts, options);
    }

    /**
     * Swaps R1 pairings if two players share a flat (strict) or tower (optimizing dispersion).
     */
    private void resolveFirstRoundConflicts(ParticipantNode[] draw, ScheduleConstraintOptions options) {
        int n = draw.length;
        for (int i = 0; i < n; i += 2) {
            ParticipantNode a = draw[i];
            ParticipantNode b = draw[i + 1];

            boolean flatConflict = options.isEnforceFlatSeparation() && flatConstraint.hasConflict(a, b);
            boolean towerConflict = options.isEnforceTowerDispersion() && towerConstraint.isSameTower(a, b);

            if (flatConflict || towerConflict) {
                // Find a swap candidate in another pairing that doesn't create new conflicts
                boolean swapped = false;
                for (int j = 0; j < n; j += 2) {
                    if (i == j) continue;
                    ParticipantNode candA = draw[j];
                    ParticipantNode candB = draw[j + 1];

                    if (!flatConstraint.hasConflict(a, candB) && !flatConstraint.hasConflict(candA, b)) {
                        // Swap b with candB
                        draw[i + 1] = candB;
                        draw[j + 1] = b;
                        swapped = true;
                        log.info("Resolved R1 conflict: Swapped {} with {}", b.getName(), candB.getName());
                        break;
                    }
                }
                if (!swapped) {
                    log.warn("Could not find ideal swap for conflict between {} and {}", a.getName(), b.getName());
                }
            }
        }
    }

    private List<ScheduledMatchNode> buildScheduledRounds(
            ParticipantNode[] initialDraw, int bracketSize,
            List<SportsCourt> courts, ScheduleConstraintOptions options) {

        List<ScheduledMatchNode> allMatches = new ArrayList<>();
        int totalRounds = (int) (Math.log(bracketSize) / Math.log(2));

        // Track court schedule availability
        Map<Long, LocalDateTime> courtAvailability = new HashMap<>();
        if (courts != null) {
            for (SportsCourt c : courts) {
                courtAvailability.put(c.getId(), options.getTournamentStartDate().atTime(options.getDayStartTime()));
            }
        }

        // Build Round 1
        int numR1Matches = bracketSize / 2;
        List<ScheduledMatchNode> currentRoundMatches = new ArrayList<>();

        for (int m = 0; m < numR1Matches; m++) {
            ParticipantNode pA = initialDraw[m * 2];
            ParticipantNode pB = initialDraw[m * 2 + 1];
            boolean isBye = (pA != null && pA.isBye()) || (pB != null && pB.isBye());

            String matchId = "R1-M" + (m + 1);
            SportsCourt assignedCourt = null;
            LocalDateTime startTime = null;
            LocalDateTime endTime = null;

            if (!isBye && courts != null && !courts.isEmpty()) {
                // Pick court with earliest availability
                assignedCourt = courts.stream()
                        .min(Comparator.comparing(c -> courtAvailability.getOrDefault(c.getId(), LocalDateTime.MIN)))
                        .orElse(courts.get(0));

                LocalDateTime currentCourtTime = courtAvailability.get(assignedCourt.getId());
                startTime = currentCourtTime;
                endTime = startTime.plusMinutes(options.getMatchDurationMinutes());

                // Advance court cursor
                courtAvailability.put(assignedCourt.getId(),
                        timeSlotConstraint.calculateNextSlot(startTime, options.getTournamentStartDate(), options));
            }

            ScheduledMatchNode matchNode = ScheduledMatchNode.builder()
                    .matchId(matchId)
                    .matchName("Match " + (m + 1))
                    .roundIndex(0)
                    .roundName(bracketConstraint.roundName(0, totalRounds))
                    .matchNumberInRound(m + 1)
                    .participantA(pA)
                    .participantB(pB)
                    .courtId(assignedCourt != null ? assignedCourt.getId() : null)
                    .courtName(assignedCourt != null ? assignedCourt.getName() : null)
                    .startTime(startTime)
                    .endTime(endTime)
                    .isByeMatch(isBye)
                    .build();

            currentRoundMatches.add(matchNode);
            allMatches.add(matchNode);
        }

        // Build Subsequent Rounds
        for (int r = 1; r < totalRounds; r++) {
            int numMatchesInRound = bracketSize / (int) Math.pow(2, r + 1);
            List<ScheduledMatchNode> nextRoundMatches = new ArrayList<>();

            for (int m = 0; m < numMatchesInRound; m++) {
                String matchId = "R" + (r + 1) + "-M" + (m + 1);
                ScheduledMatchNode prevM1 = currentRoundMatches.get(m * 2);
                ScheduledMatchNode prevM2 = currentRoundMatches.get(m * 2 + 1);

                prevM1.setNextMatchId(matchId);
                prevM2.setNextMatchId(matchId);

                SportsCourt assignedCourt = null;
                LocalDateTime startTime = null;
                LocalDateTime endTime = null;

                if (courts != null && !courts.isEmpty()) {
                    // Feature court for finals (Court 1 if available)
                    if (r == totalRounds - 1) {
                        assignedCourt = courts.get(0);
                    } else {
                        assignedCourt = courts.get(m % courts.size());
                    }

                    LocalDateTime earliestPossible = options.getTournamentStartDate().plusDays(r).atTime(options.getDayStartTime());
                    LocalDateTime courtTime = courtAvailability.getOrDefault(assignedCourt.getId(), earliestPossible);
                    startTime = courtTime.isBefore(earliestPossible) ? earliestPossible : courtTime;
                    endTime = startTime.plusMinutes(options.getMatchDurationMinutes());

                    courtAvailability.put(assignedCourt.getId(),
                            timeSlotConstraint.calculateNextSlot(startTime, options.getTournamentStartDate(), options));
                }

                ScheduledMatchNode matchNode = ScheduledMatchNode.builder()
                        .matchId(matchId)
                        .matchName("Match " + (allMatches.size() + 1))
                        .roundIndex(r)
                        .roundName(bracketConstraint.roundName(r, totalRounds))
                        .matchNumberInRound(m + 1)
                        .participantA(ParticipantNode.builder().name("Winner of " + prevM1.getMatchName()).build())
                        .participantB(ParticipantNode.builder().name("Winner of " + prevM2.getMatchName()).build())
                        .courtId(assignedCourt != null ? assignedCourt.getId() : null)
                        .courtName(assignedCourt != null ? assignedCourt.getName() : null)
                        .startTime(startTime)
                        .endTime(endTime)
                        .build();

                nextRoundMatches.add(matchNode);
                allMatches.add(matchNode);
            }
            currentRoundMatches = nextRoundMatches;
        }

        return allMatches;
    }

    /**
     * Audit validation reporting.
     */
    public ConstraintValidationReport validateSchedule(List<ScheduledMatchNode> schedule, ScheduleConstraintOptions options) {
        ConstraintValidationReport report = new ConstraintValidationReport();

        for (ScheduledMatchNode m : schedule) {
            if (m.getRoundIndex() == 0 && !m.isByeMatch()) {
                if (flatConstraint.hasConflict(m.getParticipantA(), m.getParticipantB())) {
                    report.setFlatConflicts(report.getFlatConflicts() + 1);
                    report.addViolation("Flat conflict in " + m.getMatchName() + ": both in " + m.getParticipantA().getFlatNumber());
                }
                if (towerConstraint.isSameTower(m.getParticipantA(), m.getParticipantB())) {
                    report.setTowerConflicts(report.getTowerConflicts() + 1);
                    report.addWarning("Same tower pairing in " + m.getMatchName() + ": Tower " + towerConstraint.extractTower(m.getParticipantA()));
                }
            }
        }

        courtConstraint.validateCourtOverlaps(schedule, report.getViolations());
        restConstraint.validateRestPeriods(schedule, options.getMinRestMinutes(), report.getViolations());

        report.setValid(report.getViolations().isEmpty());
        return report;
    }
}
