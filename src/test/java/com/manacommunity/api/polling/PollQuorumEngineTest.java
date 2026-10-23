package com.manacommunity.api.polling;

import com.manacommunity.api.polling.engine.PollQuorumEngine;
import com.manacommunity.api.polling.engine.PollQuorumEngine.QuorumResult;
import com.manacommunity.api.polling.engine.PollQuorumEngine.ResolutionResult;
import com.manacommunity.api.polling.engine.PollQuorumEngine.ResolutionStatus;
import com.manacommunity.api.polling.engine.PollQuorumEngine.WeightedVote;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Poll Quorum & Governance Engine Unit Tests")
class PollQuorumEngineTest {

    private final PollQuorumEngine engine = new PollQuorumEngine();

    @Test
    @DisplayName("Should validate quorum as MET when threshold reached")
    void shouldMeetQuorum() {
        // 100 members, 55 votes cast, 50% quorum required
        QuorumResult result = engine.evaluateQuorum(100, 55, 50.0);
        assertThat(result.isQuorumMet()).isTrue();
        assertThat(result.participationPercentage()).isEqualTo(55.0);
    }

    @Test
    @DisplayName("Should flag quorum as UNMET when participation is insufficient")
    void shouldFailQuorum() {
        // 200 members, 40 votes cast, 33.3% quorum required
        QuorumResult result = engine.evaluateQuorum(200, 40, 33.3);
        assertThat(result.isQuorumMet()).isFalse();
        assertThat(result.participationPercentage()).isEqualTo(20.0);
    }

    @Test
    @DisplayName("Should correctly aggregate square-footage weighted tallies")
    void shouldAggregateWeightedTallies() {
        List<WeightedVote> votes = List.of(
                new WeightedVote(1L, 1200.0), // Flat A: 1200 sqft voted Option 1
                new WeightedVote(1L, 1800.0), // Flat B: 1800 sqft voted Option 1
                new WeightedVote(2L, 1500.0)  // Flat C: 1500 sqft voted Option 2
        );

        Map<Long, Double> tallies = engine.calculateWeightedTally(votes);

        assertThat(tallies.get(1L)).isEqualTo(3000.0);
        assertThat(tallies.get(2L)).isEqualTo(1500.0);
    }

    @Test
    @DisplayName("Should pass special resolution when threshold (75%) is achieved")
    void shouldPassSpecialResolution() {
        Map<Long, Double> tallies = Map.of(10L, 8000.0, 20L, 2000.0); // 80% YES, 20% NO
        ResolutionResult result = engine.evaluateResolution(true, tallies, 10L, 20L, 75.0);

        assertThat(result.status()).isEqualTo(ResolutionStatus.PASSED);
        assertThat(result.approvalPercentage()).isEqualTo(80.0);
    }

    @Test
    @DisplayName("Should reject resolution if quorum is not met regardless of vote counts")
    void shouldRejectWhenQuorumNotMet() {
        Map<Long, Double> tallies = Map.of(10L, 9000.0, 20L, 1000.0);
        ResolutionResult result = engine.evaluateResolution(false, tallies, 10L, 20L, 50.0);

        assertThat(result.status()).isEqualTo(ResolutionStatus.QUORUM_NOT_MET);
    }
}