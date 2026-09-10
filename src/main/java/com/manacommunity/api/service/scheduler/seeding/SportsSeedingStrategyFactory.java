package com.manacommunity.api.service.scheduler.seeding;

import com.manacommunity.api.dto.scheduler.SportsPlayoffMatchDraftResponse.ParticipantRef;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Resolves the right {@link SportsSeedingStrategy} from the UI's
 * "Choose Your Schedule Sequence" selection and runs it.
 *
 * <p>Spring injects every {@link SportsSeedingStrategy} bean, so adding a new sequence
 * type is just a new {@code @Component} — no change here.</p>
 *
 * <pre>
 *   // UI sends "Random" | "Traditional" | "Sequential"
 *   List&lt;SportsPairing&gt; pairs = factory.firstRoundPairings(uiValue, players, communityRulesEnabled);
 * </pre>
 */
@Component
public class SportsSeedingStrategyFactory {

    private final Map<SportsScheduleSequence, SportsSeedingStrategy> byType = new EnumMap<>(SportsScheduleSequence.class);

    public SportsSeedingStrategyFactory(List<SportsSeedingStrategy> strategies) {
        for (SportsSeedingStrategy strategy : strategies) {
            byType.put(strategy.type(), strategy);
        }
    }

    /** The strategy bean for a resolved sequence. */
    public SportsSeedingStrategy forSequence(SportsScheduleSequence sequence) {
        SportsSeedingStrategy strategy = byType.get(sequence);
        if (strategy == null) {
            throw new com.manacommunity.api.exception.InvalidInputException("No seeding strategy registered for " + sequence);
        }
        return strategy;
    }

    /** Convenience: resolve from the raw UI string and produce the first-round pairings. */
    public List<SportsPairing> firstRoundPairings(String uiSequenceValue,
                                            List<ParticipantRef> players,
                                            boolean communityRulesEnabled) {
        SportsScheduleSequence sequence = SportsScheduleSequence.fromUi(uiSequenceValue);
        return forSequence(sequence).firstRoundPairings(players, communityRulesEnabled);
    }
}
