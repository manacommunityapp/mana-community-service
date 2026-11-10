package com.manacommunity.api.sports.dto;
import com.manacommunity.api.sports.repository.*;
import com.manacommunity.api.sports.model.*;
import com.manacommunity.api.sports.dto.*;

import java.util.List;

/**
 * Stateless input for generating a playoff ("rounds to final") bracket.
 * Mirrors the UI's PlayoffScheduleInput (playoffSchedule.ts) so the bracket
 * the server produces is identical to what the browser used to compute.
 *
 * <p>When {@link #participants} is provided (single-elimination knockout from
 * registered players), the generator uses the player-aware path: BYE
 * auto-advance, flat/tower-aware first-round pairing, and parallel court
 * allocation across {@link #courtIds}. Otherwise it uses the slot-based path.
 */
public record SportsPlayoffGenerateRequest(
    int     numGroups,
    int     proceedersPerGroup,
    String  seedingOrder,           // "TRADITIONAL" | "SEQUENTIAL" | "RANDOM"
    boolean thirdPlaceMatch,
    String  startDate,              // "yyyy-MM-dd"
    String  startTime,              // 12-hour "hh:mm AM/PM"
    int     matchDurationMinutes,
    int     breakMinutes,
    Long    venueId,
    Long    courtId,
    List<ParticipantInput> participants,  // seeded registered players (knockout-only); null/empty => slot path
    List<Long>             courtIds,       // selected courts for parallel allocation (Rule 4)
    List<BreakWindowInput> breaks,         // optional: scheduled break windows (lunch, dinner, drinks)
    String                 endTime,        // optional: daily end time limit (e.g. "08:00 PM")
    List<Integer>          matchDays,      // optional: allowed days of week (0=Sun, 6=Sat)
    java.util.Map<String, RoundTimingInput> roundTimings // optional: pinned date & start time per round
) {
    /** Backward-compatible constructor without endTime, matchDays, roundTimings. */
    public SportsPlayoffGenerateRequest(
            int numGroups, int proceedersPerGroup, String seedingOrder, boolean thirdPlaceMatch,
            String startDate, String startTime, int matchDurationMinutes, int breakMinutes,
            Long venueId, Long courtId, List<ParticipantInput> participants, List<Long> courtIds,
            List<BreakWindowInput> breaks) {
        this(numGroups, proceedersPerGroup, seedingOrder, thirdPlaceMatch, startDate, startTime,
                matchDurationMinutes, breakMinutes, venueId, courtId, participants, courtIds, breaks,
                null, null, null);
    }

    /** Backward-compatible constructor without breaks, endTime, matchDays, roundTimings. */
    public SportsPlayoffGenerateRequest(
            int numGroups, int proceedersPerGroup, String seedingOrder, boolean thirdPlaceMatch,
            String startDate, String startTime, int matchDurationMinutes, int breakMinutes,
            Long venueId, Long courtId, List<ParticipantInput> participants, List<Long> courtIds) {
        this(numGroups, proceedersPerGroup, seedingOrder, thirdPlaceMatch, startDate, startTime,
                matchDurationMinutes, breakMinutes, venueId, courtId, participants, courtIds, null,
                null, null, null);
    }

    /**
     * Pinned date, start time, and optional court overrides for a tournament round
     * (e.g. QUARTER_FINAL, SEMI_FINAL, FINAL, THIRD_PLACE).
     */
    public record RoundTimingInput(
            String date,
            String startTime,
            List<Long> courtIds
    ) {}

    /**
     * A break window (lunch, dinner, tea/drinks) during which no matches should be scheduled.
     */
    public record BreakWindowInput(
            String  id,
            String  name,
            String  type,
            String  startTime,
            String  endTime,
            Integer durationMinutes,
            Boolean enabled
    ) {}

    /**
     * A seeded player participating directly in a knockout draw.
     * {@code rating} is the optional skill rating used for balanced pairing
     * (Rule 6); null when the sport/registration carries no rating.
     */
    public record ParticipantInput(String id, String name, String flatNumber, Integer rating) {
        /** Back-compat constructor for callers without a skill rating. */
        public ParticipantInput(String id, String name, String flatNumber) {
            this(id, name, flatNumber, null);
        }
    }
}
