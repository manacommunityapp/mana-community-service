package com.manacommunity.api.sports.scheduler;
import com.manacommunity.api.sports.dto.SportsPlayoffGenerateRequest.ParticipantInput;
import com.manacommunity.api.sports.dto.SportsPlayoffMatchDraftResponse.ParticipantRef;
import com.manacommunity.api.sports.repository.*;
import com.manacommunity.api.sports.model.*;
import com.manacommunity.api.sports.dto.*;
import com.manacommunity.api.sports.scheduler.seeding.SportsPairing;


import com.manacommunity.api.sports.dto.SportsPlayoffGenerateRequest;
import com.manacommunity.api.sports.dto.SportsPlayoffMatchDraftResponse;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Stateless generator for playoff ("rounds to final") brackets.
 *
 * Faithful Java port of the frontend's playoffSchedule.ts so the server
 * reproduces the exact bracket the browser used to compute locally
 * (same pairings, round names, dates and 12-hour times).
 */
@Component
public class SportsPlayoffScheduleGenerator {

    private static final Random RANDOM = new Random();

    // ── Slot / participant helpers ────────────────────────────────────

    /** A group proceeder slot (e.g. "Winner 1 of Group 2"). */
    private record Slot(int groupIndex, int rank) {}

    private static String proceederLabel(int groupIndex, int rank) {
        return "Winner " + rank + " of Group " + (groupIndex + 1);
    }

    private static String matchWinnerLabel(String matchName) {
        return matchName + " (Winner)";
    }

    private static String slotId(int groupIndex, int rank) {
        return "G" + (groupIndex + 1) + "-W" + rank;
    }

    private static List<Slot> buildProceederSlots(int numGroups, int proceedersPerGroup) {
        List<Slot> slots = new ArrayList<>();
        for (int g = 0; g < numGroups; g++) {
            for (int r = 1; r <= proceedersPerGroup; r++) {
                slots.add(new Slot(g, r));
            }
        }
        return slots;
    }

    private static <T> List<T> shuffle(List<T> items) {
        List<T> copy = new ArrayList<>(items);
        Collections.shuffle(copy, RANDOM);
        return copy;
    }

    /**
     * Randomly chooses the index of the player to receive the BYE this round.
     * Prefers players who have not had a bye yet so the bye is shuffled fairly
     * across rounds; if every player in the round has already had one, it picks
     * randomly from the whole round.
     */
    private static int pickByeIndex(List<ParticipantRef> roundParticipants, Set<String> byeReceived) {
        List<Integer> eligible = new ArrayList<>();
        for (int i = 0; i < roundParticipants.size(); i++) {
            if (!byeReceived.contains(roundParticipants.get(i).id())) {
                eligible.add(i);
            }
        }
        if (eligible.isEmpty()) {
            for (int i = 0; i < roundParticipants.size(); i++) {
                eligible.add(i);
            }
        }
        return eligible.get(RANDOM.nextInt(eligible.size()));
    }

    private static ParticipantRef toSlotParticipant(Slot slot) {
        return new ParticipantRef(slotId(slot.groupIndex(), slot.rank()),
                proceederLabel(slot.groupIndex(), slot.rank()));
    }

    private static ParticipantRef winnerRef(String matchId, String matchName) {
        return new ParticipantRef(matchId + "-winner", matchWinnerLabel(matchName));
    }

    private static ParticipantRef loserRef(String matchId, String matchName) {
        return new ParticipantRef(matchId + "-loser", "Loser Of " + matchName);
    }

    // ── SportsPairing logic ─────────────────────────────────────────────────

    /** First-round pairings from group proceeder slots. */
    static List<Slot[]> buildFirstRoundPairings(List<Slot> slots, String seedingOrder, int numGroups) {
        if (slots.size() < 2) return new ArrayList<>();

        if ("SEQUENTIAL".equals(seedingOrder) && numGroups >= 2) {
            List<Slot[]> pairs = new ArrayList<>();
            for (int g = 0; g < numGroups; g += 2) {
                final int gi = g;
                List<Slot> left  = slots.stream().filter(s -> s.groupIndex() == gi).toList();
                List<Slot> right = slots.stream().filter(s -> s.groupIndex() == gi + 1).toList();
                int len = Math.min(left.size(), right.size());
                for (int i = 0; i < len; i++) {
                    pairs.add(new Slot[]{left.get(i), right.get(i)});
                }
            }
            if (!pairs.isEmpty()) return pairs;
        }

        List<Slot> ordered = new ArrayList<>(slots);
        if ("RANDOM".equals(seedingOrder)) {
            ordered = shuffle(ordered);
        }

        // TRADITIONAL (and RANDOM after shuffle): 1 vs N, 2 vs N-1 …
        List<Slot[]> pairs = new ArrayList<>();
        int lo = 0;
        int hi = ordered.size() - 1;
        while (lo < hi) {
            pairs.add(new Slot[]{ordered.get(lo), ordered.get(hi)});
            lo++;
            hi--;
        }
        return pairs;
    }

    // ── Round naming ──────────────────────────────────────────────────

    public static String getRoundName(int roundIndex, int totalRounds) {
        int roundsFromFinal = totalRounds - 1 - roundIndex;
        return switch (roundsFromFinal) {
            case 0 -> "FINAL";
            case 1 -> "SEMI_FINAL";
            case 2 -> "QUARTER_FINAL";
            case 3 -> "ROUND_OF_16";
            case 4 -> "ROUND_OF_32";
            case 5 -> "ROUND_OF_64";
            default -> "Round " + (roundIndex + 1);
        };
    }

    private static String roundLabel(String type, int indexInRound) {
        return switch (type) {
            case "ROUND_OF_64"   -> "Round of 64 Match " + (indexInRound + 1);
            case "ROUND_OF_32"   -> "Round of 32 Match " + (indexInRound + 1);
            case "ROUND_OF_16"   -> "Round of 16 Match " + (indexInRound + 1);
            case "QUARTER_FINAL" -> "Quarter Final " + (indexInRound + 1);
            case "SEMI_FINAL"    -> "Semi-Final " + (indexInRound + 1);
            case "FINAL"         -> "Final";
            case "THIRD_PLACE"   -> "Third Place";
            default              -> type + " Match " + (indexInRound + 1);
        };
    }

    // ── 12-hour time helpers (must match playoffSchedule.ts exactly) ───

    private static final Pattern TIME_12H =
            Pattern.compile("^(\\d{1,2}):(\\d{2})\\s*(AM|PM)$", Pattern.CASE_INSENSITIVE);

    /** {hours, minutes}; falls back to 08:00 when unparseable (mirrors TS). */
    static int[] parseTime12h(String time) {
        if (time == null) return new int[]{8, 0};
        Matcher m = TIME_12H.matcher(time.trim());
        if (!m.matches()) return new int[]{8, 0};
        int hours = Integer.parseInt(m.group(1));
        int minutes = Integer.parseInt(m.group(2));
        String meridiem = m.group(3).toUpperCase();
        if (meridiem.equals("PM") && hours != 12) hours += 12;
        if (meridiem.equals("AM") && hours == 12) hours = 0;
        return new int[]{hours, minutes};
    }

    static String formatTime12h(int hours, int minutes) {
        String meridiem = hours >= 12 ? "PM" : "AM";
        int h = hours % 12;
        if (h == 0) h = 12;
        return pad2(h) + ":" + pad2(minutes) + " " + meridiem;
    }

    public static String addMinutesToTime(String time, int minutesToAdd) {
        int[] hm = parseTime12h(time);
        int total = hm[0] * 60 + hm[1] + minutesToAdd;
        int newHours = (total / 60) % 24;
        int newMinutes = total % 60;
        return formatTime12h(newHours, newMinutes);
    }

    static String addDaysToDateString(String dateStr, int days) {
        String[] parts = dateStr.split("-");
        LocalDate d = parts.length == 3
                ? LocalDate.of(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]), Integer.parseInt(parts[2]))
                : LocalDate.parse(dateStr);
        return d.plusDays(days).toString();
    }

    static String advanceDateToAllowedDay(String dateStr, List<Integer> allowedDays) {
        if (allowedDays == null || allowedDays.isEmpty() || allowedDays.size() == 7) {
            return dateStr;
        }
        LocalDate d = LocalDate.parse(dateStr);
        int guard = 0;
        // DayOfWeek: Monday=1..Sunday=7. Modulo 7 gives 0 for Sunday, 1 for Monday, ..., 6 for Saturday.
        while (!allowedDays.contains(d.getDayOfWeek().getValue() % 7) && guard < 14) {
            d = d.plusDays(1);
            guard++;
        }
        return d.toString();
    }

    private static String pad2(int n) {
        return n < 10 ? "0" + n : String.valueOf(n);
    }

    static int parseTimeToMinutes(String timeStr) {
        if (timeStr == null || timeStr.isBlank()) return 0;
        int[] h12 = parseTime12h(timeStr);
        return h12[0] * 60 + h12[1];
    }

    static String skipBreaksIfOverlapping(String timeStr, int matchDurationMinutes, List<SportsPlayoffGenerateRequest.BreakWindowInput> breaks) {
        if (breaks == null || breaks.isEmpty()) return timeStr;
        String curTime = timeStr;
        int guard = 0;
        while (guard < 10) {
            int slotStart = parseTimeToMinutes(curTime);
            int slotEnd = slotStart + matchDurationMinutes;
            SportsPlayoffGenerateRequest.BreakWindowInput conflict = null;
            for (SportsPlayoffGenerateRequest.BreakWindowInput b : breaks) {
                if (b == null || Boolean.FALSE.equals(b.enabled())) continue;
                int bStart = parseTimeToMinutes(b.startTime());
                int bEnd = parseTimeToMinutes(b.endTime());
                if (Math.max(slotStart, bStart) < Math.min(slotEnd, bEnd)) {
                    conflict = b;
                    break;
                }
            }
            if (conflict == null) break;
            curTime = conflict.endTime();
            guard++;
        }
        return curTime;
    }

    /** Sequential time/date cursor; respects day bounds (endTime), allowed days (weekends/weekdays), and breaks. */
    private static final class ScheduleCursor {
        private String slotDate;
        private String slotTime;
        private final String dayStartTime;
        private final int step;
        private final int matchDuration;
        private final List<SportsPlayoffGenerateRequest.BreakWindowInput> breaks;
        private final int dayEndMin;
        private final List<Integer> matchDays;

        ScheduleCursor(String startDate, String startTime, String endTime, int matchDurationMinutes, int breakMinutes,
                       List<Integer> matchDays, List<SportsPlayoffGenerateRequest.BreakWindowInput> breaks) {
            this.matchDuration = matchDurationMinutes;
            this.breaks = breaks;
            this.dayStartTime = startTime;
            this.step = matchDurationMinutes + breakMinutes;
            this.matchDays = matchDays;
            this.dayEndMin = (endTime != null && !endTime.isBlank()) ? parseTimeToMinutes(endTime) : 1440;
            this.slotDate = advanceDateToAllowedDay(startDate, matchDays);
            this.slotTime = skipBreaksIfOverlapping(startTime, matchDurationMinutes, breaks);
        }

        void anchor(String newDate, String newTime) {
            if (newDate != null && !newDate.isBlank()) {
                this.slotDate = advanceDateToAllowedDay(newDate, matchDays);
            }
            if (newTime != null && !newTime.isBlank()) {
                this.slotTime = skipBreaksIfOverlapping(newTime, matchDuration, breaks);
            }
        }

        String[] next() {
            slotTime = skipBreaksIfOverlapping(slotTime, matchDuration, breaks);
            if (parseTimeToMinutes(slotTime) + matchDuration > dayEndMin) {
                rollToNextDay();
            }
            String[] current = {slotDate, slotTime};
            slotTime = addMinutesToTime(slotTime, step);
            slotTime = skipBreaksIfOverlapping(slotTime, matchDuration, breaks);
            if (parseTimeToMinutes(slotTime) + matchDuration > dayEndMin || parseTime12h(slotTime)[0] < 6) {
                rollToNextDay();
            }
            return current;
        }

        private void rollToNextDay() {
            slotDate = advanceDateToAllowedDay(addDaysToDateString(slotDate, 1), matchDays);
            slotTime = skipBreaksIfOverlapping(dayStartTime, matchDuration, breaks);
        }

        /** The day the next slot falls on, without consuming/advancing the cursor. */
        String currentDate() {
            return slotDate;
        }
    }

    private static void anchorRound(ScheduleCursor cursor, Map<String, SportsPlayoffGenerateRequest.RoundTimingInput> timings, String roundType) {
        if (timings == null || roundType == null) return;
        SportsPlayoffGenerateRequest.RoundTimingInput t = timings.get(roundType);
        if (t != null) {
            cursor.anchor(t.date(), t.startTime());
        }
    }

    private static void anchorRound(WaveScheduler sched, Map<String, SportsPlayoffGenerateRequest.RoundTimingInput> timings, String roundType) {
        if (timings == null || roundType == null) return;
        SportsPlayoffGenerateRequest.RoundTimingInput t = timings.get(roundType);
        if (t != null) {
            sched.anchor(t.date(), t.startTime());
        }
    }

    // ── Bracket builders ──────────────────────────────────────────────

    /** Standard 2 groups × 2 proceeders → cross semi-finals + final. */
    private static List<SportsPlayoffMatchDraftResponse> buildTwoGroupTwoProceederBracket(SportsPlayoffGenerateRequest in) {
        ScheduleCursor cursor = new ScheduleCursor(
                in.startDate(), in.startTime(), in.endTime(), in.matchDurationMinutes(), in.breakMinutes(),
                in.matchDays(), in.breaks());

        Slot[] sf1Pair;
        Slot[] sf2Pair;
        if ("SEQUENTIAL".equals(in.seedingOrder())) {
            sf1Pair = new Slot[]{new Slot(0, 1), new Slot(1, 1)};
            sf2Pair = new Slot[]{new Slot(0, 2), new Slot(1, 2)};
        } else {
            sf1Pair = new Slot[]{new Slot(0, 1), new Slot(1, 2)};
            sf2Pair = new Slot[]{new Slot(0, 2), new Slot(1, 1)};
        }

        String sf1Id = "playoff-sf1";
        String sf2Id = "playoff-sf2";
        String finalId = "playoff-final";

        anchorRound(cursor, in.roundTimings(), "SEMI_FINAL");
        String[] s1 = cursor.next();
        String[] s2 = cursor.next();

        anchorRound(cursor, in.roundTimings(), "FINAL");
        String[] sFinal = cursor.next();

        List<SportsPlayoffMatchDraftResponse> matches = new ArrayList<>();
        matches.add(new SportsPlayoffMatchDraftResponse(sf1Id, "Semi-Final 1", "SEMI_FINAL", 0,
                toSlotParticipant(sf1Pair[0]), toSlotParticipant(sf1Pair[1]),
                s1[0], s1[1], in.matchDurationMinutes(), in.venueId(), in.courtId(), false, null));
        matches.add(new SportsPlayoffMatchDraftResponse(sf2Id, "Semi-Final 2", "SEMI_FINAL", 0,
                toSlotParticipant(sf2Pair[0]), toSlotParticipant(sf2Pair[1]),
                s2[0], s2[1], in.matchDurationMinutes(), in.venueId(), in.courtId(), false, null));
        matches.add(new SportsPlayoffMatchDraftResponse(finalId, "Final", "FINAL", 1,
                winnerRef(sf1Id, "Semi-Final 1"), winnerRef(sf2Id, "Semi-Final 2"),
                sFinal[0], sFinal[1], in.matchDurationMinutes(), in.venueId(), in.courtId(), false, null));

        if (in.thirdPlaceMatch()) {
            anchorRound(cursor, in.roundTimings(), "THIRD_PLACE");
            String[] s3 = cursor.next();
            matches.add(new SportsPlayoffMatchDraftResponse("playoff-third-place", "Third Place", "THIRD_PLACE", 2,
                    loserRef(sf1Id, "Semi-Final 1"), loserRef(sf2Id, "Semi-Final 2"),
                    s3[0], s3[1], in.matchDurationMinutes(), in.venueId(), in.courtId(), false, null));
        }
        return matches;
    }

    public List<SportsPlayoffMatchDraftResponse> buildPlayoffBracket(SportsPlayoffGenerateRequest in) {
        // Player-aware path: single-elimination knockout from seeded registered players
        // (BYE auto-advance, flat/tower-aware first round, parallel court allocation).
        if (in.participants() != null && in.participants().size() >= 2) {
            return buildKnockoutFromPlayers(in);
        }
        if (in.numGroups() == 2 && in.proceedersPerGroup() == 2) {
            return buildTwoGroupTwoProceederBracket(in);
        }

        List<Slot> slots = buildProceederSlots(in.numGroups(), in.proceedersPerGroup());
        if (slots.size() < 2) return new ArrayList<>();

        List<Slot[]> firstRoundPairs = buildFirstRoundPairings(slots, in.seedingOrder(), in.numGroups());
        List<ParticipantRef> current = new ArrayList<>();
        for (Slot[] pair : firstRoundPairs) {
            current.add(toSlotParticipant(pair[0]));
            current.add(toSlotParticipant(pair[1]));
        }

        // Add any unpaired leftover slots (the BYEs in round 1)
        Set<String> pairedIds = new HashSet<>();
        current.forEach(p -> pairedIds.add(p.id()));
        for (Slot slot : slots) {
            ParticipantRef p = toSlotParticipant(slot);
            if (!pairedIds.contains(p.id())) {
                current.add(p);
            }
        }

        // Simulate round sizes to find total rounds
        int simLength = current.size();
        int totalRounds = 0;
        while (simLength > 1) {
            totalRounds++;
            simLength = (int) Math.ceil(simLength / 2.0);
        }

        List<SportsPlayoffMatchDraftResponse> matches = new ArrayList<>();
        int roundIndex = 0;
        List<ParticipantRef> roundParticipants = new ArrayList<>(current);
        ScheduleCursor cursor = new ScheduleCursor(
                in.startDate(), in.startTime(), in.endTime(), in.matchDurationMinutes(), in.breakMinutes(),
                in.matchDays(), in.breaks());

        // Track who has already received a BYE so the bye rotates fairly across
        // rounds (a player should not get a bye again until everyone else has).
        Set<String> byeReceived = new HashSet<>();

        while (roundParticipants.size() > 1) {
            String roundType = getRoundName(roundIndex, totalRounds);
            anchorRound(cursor, in.roundTimings(), roundType);
            List<ParticipantRef> nextParticipants = new ArrayList<>();
            int len = roundParticipants.size();

            if (len % 2 != 0) {
                // Randomly pick the BYE among players who have NOT had one yet;
                // only if everyone in this round already has, fall back to all.
                int byeIdx = pickByeIndex(roundParticipants, byeReceived);
                ParticipantRef byeParticipant = roundParticipants.get(byeIdx);
                byeReceived.add(byeParticipant.id());

                List<ParticipantRef> activeParticipants = new ArrayList<>();
                for (int i = 0; i < len; i++) {
                    if (i != byeIdx) activeParticipants.add(roundParticipants.get(i));
                }
                int numMatches = activeParticipants.size() / 2;

                for (int mIdx = 0; mIdx < numMatches; mIdx++) {
                    ParticipantRef home = activeParticipants.get(2 * mIdx);
                    ParticipantRef away = activeParticipants.get(2 * mIdx + 1);
                    String id = "playoff-r" + roundIndex + "-m" + mIdx;
                    String name = roundLabel(roundType, mIdx);
                    String[] slot = cursor.next();
                    matches.add(new SportsPlayoffMatchDraftResponse(id, name, roundType, roundIndex,
                            home, away, slot[0], slot[1],
                            in.matchDurationMinutes(), in.venueId(), in.courtId(), false, null));
                    nextParticipants.add(winnerRef(id, name));
                }

                // Generate the BYE match at the end of the round.
                // A bye isn't played, so it does NOT consume a time slot (the cursor is
                // not advanced) and carries no scheduled time — only the day, for display.
                int byeMatchIdx = numMatches;
                String byeMatchId = "playoff-r" + roundIndex + "-m" + byeMatchIdx;
                String byeMatchName = byeParticipant.name() + " (BYE)";
                matches.add(new SportsPlayoffMatchDraftResponse(byeMatchId, byeMatchName, roundType, roundIndex,
                        byeParticipant, new ParticipantRef("bye", "BYE"), cursor.currentDate(), "",
                        in.matchDurationMinutes(), in.venueId(), in.courtId(), false, null));

                // Advance the BYE player directly (preserving clean name)
                nextParticipants.add(byeParticipant);
            } else {
                int numMatches = len / 2;
                for (int mIdx = 0; mIdx < numMatches; mIdx++) {
                    ParticipantRef home = roundParticipants.get(2 * mIdx);
                    ParticipantRef away = roundParticipants.get(2 * mIdx + 1);
                    String id = "playoff-r" + roundIndex + "-m" + mIdx;
                    String name = roundLabel(roundType, mIdx);
                    String[] slot = cursor.next();
                    matches.add(new SportsPlayoffMatchDraftResponse(id, name, roundType, roundIndex,
                            home, away, slot[0], slot[1],
                            in.matchDurationMinutes(), in.venueId(), in.courtId(), false, null));
                    nextParticipants.add(winnerRef(id, name));
                }
            }

            roundParticipants = nextParticipants;
            roundIndex++;
        }

        if (in.thirdPlaceMatch()) {
            anchorRound(cursor, in.roundTimings(), "THIRD_PLACE");
            List<SportsPlayoffMatchDraftResponse> semiMatches =
                    matches.stream().filter(m -> "SEMI_FINAL".equals(m.round())).toList();
            if (semiMatches.size() >= 2) {
                String[] slot = cursor.next();
                matches.add(new SportsPlayoffMatchDraftResponse("playoff-third-place", "Third Place",
                        "THIRD_PLACE", roundIndex,
                        loserRef(semiMatches.get(0).id(), semiMatches.get(0).name()),
                        loserRef(semiMatches.get(1).id(), semiMatches.get(1).name()),
                        slot[0], slot[1], in.matchDurationMinutes(), in.venueId(), in.courtId(), false, null));
            }
        }

        return matches;
    }

    // ══════════════════════════════════════════════════════════════════
    // PLAYER-AWARE KNOCKOUT (single elimination from seeded registrations)
    // ══════════════════════════════════════════════════════════════════

    public List<SportsPlayoffMatchDraftResponse> buildKnockoutFromPlayers(SportsPlayoffGenerateRequest in) {
        List<ParticipantInput> players = in.participants();
        int n = players.size();
        if (n < 2) return new ArrayList<>();

        // Round-1 seed list (carries flat number for Rule 1/2 clash checks).
        List<ParticipantRef> seeds = new ArrayList<>();
        Map<String, Integer> ratingById = new HashMap<>();
        boolean haveRatings = false;
        for (ParticipantInput p : players) {
            seeds.add(new ParticipantRef(p.id(), p.name(), p.flatNumber()));
            if (p.rating() != null) {
                ratingById.put(p.id(), p.rating());
                haveRatings = true;
            }
        }

        // Rule 6 — balance skill levels: order round-1 players by rating so that
        // similar ratings meet (5 vs 4, not 5 vs 1); neighbours are then paired.
        if (haveRatings) {
            seeds.sort(Comparator.comparingInt(
                    (ParticipantRef r) -> ratingById.getOrDefault(r.id(), Integer.MIN_VALUE)).reversed());
        }

        // Rule 3 — total rounds = ceil(log2(n)); exactly ONE bye is injected on
        // each round whose participant count is odd (cascading down the bracket).
        int totalRounds = 0;
        for (int s = 1; s < n; s <<= 1) totalRounds++;

        List<Long> courtIds = new ArrayList<>();
        if (in.courtIds() != null) courtIds.addAll(in.courtIds());
        if (courtIds.isEmpty() && in.courtId() != null) courtIds.add(in.courtId());

        WaveScheduler sched = new WaveScheduler(
                in.startDate(), in.startTime(), in.endTime(), in.matchDurationMinutes(), in.breakMinutes(),
                courtIds, in.matchDays(), in.breaks());

        List<SportsPlayoffMatchDraftResponse> matches = new ArrayList<>();
        Set<String> byeReceived = new HashSet<>();   // Rule 4 — avoid repeat byes
        List<ParticipantRef> roundParticipants = new ArrayList<>(seeds);
        int roundIndex = 0;

        while (roundParticipants.size() > 1) {
            String roundType = getRoundName(roundIndex, totalRounds);
            anchorRound(sched, in.roundTimings(), roundType);
            List<ParticipantRef> active = new ArrayList<>(roundParticipants);
            List<ParticipantRef> next = new ArrayList<>();

            // Rule 3 + 4: when the round has an odd count, pull exactly ONE bye,
            // chosen randomly and preferring players who have not had a bye yet.
            ParticipantRef byePlayer = null;
            if (active.size() % 2 != 0) {
                int byeIdx = pickByeIndex(active, byeReceived);
                byePlayer = active.remove(byeIdx);
                byeReceived.add(byePlayer.id());
            }

            // Pair the remaining (even) participants. Round 0 uses standard tournament bracket
            // seeding so Seed 1 is in Match 0 (Top Half) and Seed 2 is in the bottom Match (Bottom Half);
            // later rounds pair bracket winners.
            List<ParticipantRef[]> pairs = (roundIndex == 0) ? tournamentSeededPairs(active) : neighborPairs(active);
            if (roundIndex == 0) applyConstraints(pairs);   // Rules 1 & 2 (first round only)

            int mIdx = 0;
            for (ParticipantRef[] pr : pairs) {
                String id = "ko-r" + roundIndex + "-m" + mIdx;
                String name = roundLabel(roundType, mIdx);
                TimedSlot s = sched.nextSlot();
                matches.add(new SportsPlayoffMatchDraftResponse(id, name, roundType, roundIndex,
                        pr[0], pr[1], s.date(), s.time(), in.matchDurationMinutes(),
                        in.venueId(), s.courtId(), false, null));
                next.add(winnerRef(id, name));
                mIdx++;
            }

            // Rule 5: the BYE match is not played — no court, no time, AUTO_ADVANCED;
            // the bye player advances directly to the next round.
            if (byePlayer != null) {
                String byeId = "ko-r" + roundIndex + "-bye";
                matches.add(new SportsPlayoffMatchDraftResponse(byeId, byePlayer.name() + " (BYE)",
                        roundType, roundIndex, byePlayer, new ParticipantRef("bye", "BYE"),
                        sched.currentDate(), "", in.matchDurationMinutes(),
                        in.venueId(), null, false, "AUTO_ADVANCED"));
                next.add(byePlayer);
            }

            sched.flushWave();   // Rules 7 & 8 — fresh time-slot wave per round boundary
            roundParticipants = next;
            roundIndex++;
        }

        // Optional third-place match (losers of the two semi-finals).
        if (in.thirdPlaceMatch()) {
            anchorRound(sched, in.roundTimings(), "THIRD_PLACE");
            List<SportsPlayoffMatchDraftResponse> semis =
                    matches.stream().filter(m -> "SEMI_FINAL".equals(m.round())).toList();
            if (semis.size() >= 2) {
                TimedSlot s = sched.nextSlot();
                matches.add(new SportsPlayoffMatchDraftResponse("ko-third-place", "Third Place",
                        "THIRD_PLACE", roundIndex,
                        loserRef(semis.get(0).id(), semis.get(0).name()),
                        loserRef(semis.get(1).id(), semis.get(1).name()),
                        s.date(), s.time(), in.matchDurationMinutes(),
                        in.venueId(), s.courtId(), false, null));
            }
        }

        return matches;
    }

    /**
     * Standard tournament bracket seeding for Round 1:
     * - Top Half: Seed 1 (Match 0), Seed 4
     * - Bottom Half: Seed 3, Seed 2 (Bottom Match)
     * Seed 1 and Seed 2 are placed in opposite halves and can only meet in the FINAL.
     */
    private static List<ParticipantRef[]> tournamentSeededPairs(List<ParticipantRef> players) {
        int total = players.size();
        if (total <= 2) {
            return neighborPairs(players);
        }

        // Build power-of-2 standard tournament bracket permutation
        int powerOf2 = 1;
        while (powerOf2 < total) {
            powerOf2 <<= 1;
        }

        List<Integer> bracket = new ArrayList<>();
        bracket.add(1);
        bracket.add(2);

        while (bracket.size() < powerOf2) {
            int nextSum = bracket.size() * 2 + 1;
            List<Integer> nextBracket = new ArrayList<>();
            for (int i = 0; i < bracket.size(); i += 2) {
                int a = bracket.get(i);
                int b = bracket.get(i + 1);
                nextBracket.add(a);
                nextBracket.add(nextSum - a);
                nextBracket.add(b);
                nextBracket.add(nextSum - b);
            }
            bracket = nextBracket;
        }

        // Map bracket seeds (1-based) to available player indices
        List<ParticipantRef[]> pairs = new ArrayList<>();
        for (int i = 0; i < bracket.size(); i += 2) {
            int idx1 = bracket.get(i) - 1;
            int idx2 = bracket.get(i + 1) - 1;
            if (idx1 < total && idx2 < total) {
                pairs.add(new ParticipantRef[]{players.get(idx1), players.get(idx2)});
            }
        }

        if (pairs.size() * 2 == total) {
            return pairs;
        }

        // Fallback for irregular count: pair (i) with (total - 1 - i)
        pairs.clear();
        int half = total / 2;
        for (int i = 0; i < half; i++) {
            int home = (i % 2 == 0) ? (i / 2) : (half - 1 - (i / 2));
            int away = total - 1 - home;
            pairs.add(new ParticipantRef[]{players.get(home), players.get(away)});
        }
        return pairs;
    }

    /** Pair an even-sized list as neighbours: (0,1),(2,3),… (for placeholder rounds). */
    private static List<ParticipantRef[]> neighborPairs(List<ParticipantRef> players) {
        List<ParticipantRef[]> pairs = new ArrayList<>();
        for (int i = 0; i + 1 < players.size(); i += 2) {
            pairs.add(new ParticipantRef[]{players.get(i), players.get(i + 1)});
        }
        return pairs;
    }

    /**
     * Best-effort first-round constraint pass: for any pair where the two players share a flat
     * (Rule 1) or a tower (Rule 2), try swapping the away players with another pair so both pairs
     * become conflict-free. Leaves the pair unchanged if no valid swap exists.
     */
    private static void applyConstraints(List<ParticipantRef[]> pairs) {
        for (int i = 0; i < pairs.size(); i++) {
            if (!conflict(pairs.get(i)[0], pairs.get(i)[1])) continue;
            for (int j = 0; j < pairs.size(); j++) {
                if (j == i) continue;
                ParticipantRef awayI = pairs.get(i)[1];
                ParticipantRef awayJ = pairs.get(j)[1];
                if (!conflict(pairs.get(i)[0], awayJ) && !conflict(pairs.get(j)[0], awayI)) {
                    pairs.get(i)[1] = awayJ;
                    pairs.get(j)[1] = awayI;
                    break;
                }
            }
        }
    }

    /** Two players conflict when they share a flat (Rule 1) or a tower (Rule 2). */
    private static boolean conflict(ParticipantRef a, ParticipantRef b) {
        String fa = norm(a.flatNumber());
        String fb = norm(b.flatNumber());
        if (!fa.isEmpty() && fa.equals(fb)) return true;          // same flat
        String ta = tower(a.flatNumber());
        String tb = tower(b.flatNumber());
        return !ta.isEmpty() && ta.equals(tb);                    // same tower
    }

    /** Leading letters of a flat number (e.g. "A302" → "A", "A-305" → "A", "302" → ""). */
    private static String tower(String flat) {
        if (flat == null) return "";
        StringBuilder sb = new StringBuilder();
        for (char c : flat.trim().toCharArray()) {
            if (Character.isLetter(c)) sb.append(Character.toUpperCase(c));
            else break;
        }
        return sb.toString();
    }

    private static String norm(String s) {
        return s == null ? "" : s.trim().toLowerCase();
    }

    private record TimedSlot(String date, String time, Long courtId) {}

    /**
     * Parallel court allocator (Rule 4): assigns up to {@code courtIds.size()} matches at the same
     * time across the available courts, then advances one wave (duration + break).
     */
    private static final class WaveScheduler {
        private String date;
        private String time;
        private final String dayStart;
        private final int step;
        private final List<Long> courtIds;
        private int courtPtr = 0;
        private final int matchDuration;
        private final List<SportsPlayoffGenerateRequest.BreakWindowInput> breaks;
        private final int dayEndMin;
        private final List<Integer> matchDays;

        WaveScheduler(String startDate, String startTime, String endTime, int durationMinutes, int breakMinutes,
                      List<Long> courtIds, List<Integer> matchDays, List<SportsPlayoffGenerateRequest.BreakWindowInput> breaks) {
            this.matchDuration = durationMinutes;
            this.breaks = breaks;
            this.dayStart = startTime;
            this.step = durationMinutes + breakMinutes;
            this.courtIds = courtIds;
            this.matchDays = matchDays;
            this.dayEndMin = (endTime != null && !endTime.isBlank()) ? parseTimeToMinutes(endTime) : 1440;
            this.date = advanceDateToAllowedDay(startDate, matchDays);
            this.time = skipBreaksIfOverlapping(startTime, durationMinutes, breaks);
        }

        void anchor(String newDate, String newTime) {
            if (newDate != null && !newDate.isBlank()) {
                this.date = advanceDateToAllowedDay(newDate, matchDays);
            }
            if (newTime != null && !newTime.isBlank()) {
                this.time = skipBreaksIfOverlapping(newTime, matchDuration, breaks);
            }
            this.courtPtr = 0;
        }

        TimedSlot nextSlot() {
            time = skipBreaksIfOverlapping(time, matchDuration, breaks);
            if (parseTimeToMinutes(time) + matchDuration > dayEndMin) {
                rollToNextDay();
                courtPtr = 0;
            }
            Long court = courtIds.isEmpty() ? null : courtIds.get(courtPtr % courtIds.size());
            TimedSlot slot = new TimedSlot(date, time, court);
            courtPtr++;
            if (courtPtr >= Math.max(1, courtIds.size())) {
                advance();
                courtPtr = 0;
            }
            return slot;
        }

        /** Start a fresh wave at a round boundary so rounds don't share a time slot. */
        void flushWave() {
            if (courtPtr > 0) {
                advance();
                courtPtr = 0;
            }
        }

        String currentDate() {
            return date;
        }

        private void advance() {
            time = addMinutesToTime(time, step);
            time = skipBreaksIfOverlapping(time, matchDuration, breaks);
            if (parseTimeToMinutes(time) + matchDuration > dayEndMin || parseTime12h(time)[0] < 6) {
                rollToNextDay();
            }
        }

        private void rollToNextDay() {
            date = advanceDateToAllowedDay(addDaysToDateString(date, 1), matchDays);
            time = skipBreaksIfOverlapping(dayStart, matchDuration, breaks);
        }
    }
}
