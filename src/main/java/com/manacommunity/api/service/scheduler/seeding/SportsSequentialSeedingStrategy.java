package com.manacommunity.api.service.scheduler.seeding;

import com.manacommunity.api.model.Community;

import com.manacommunity.api.dto.scheduler.SportsPlayoffMatchDraftResponse.ParticipantRef;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * SEQUENTIAL — pair players strictly in registration order, with no randomization:
 * <pre>
 *   P1 vs P2
 *   P3 vs P4
 *   P5 vs P6
 *   P7 vs P8
 * </pre>
 * An odd number of players leaves the last one with a BYE. Community rules are
 * intentionally ignored: the order is fixed by registration.
 */
@Component
public class SportsSequentialSeedingStrategy implements SportsSeedingStrategy {

    @Override
    public SportsScheduleSequence type() {
        return SportsScheduleSequence.SEQUENTIAL;
    }

    @Override
    public List<SportsPairing> firstRoundPairings(List<ParticipantRef> players, boolean communityRulesEnabled) {
        // Registration order is authoritative — pair neighbours as-is.
        return SportsSeedingStrategy.neighborPairs(players);
    }
}
