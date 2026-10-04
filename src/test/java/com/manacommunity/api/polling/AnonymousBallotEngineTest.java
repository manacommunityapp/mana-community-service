package com.manacommunity.api.polling;

import com.manacommunity.api.polling.engine.AnonymousBallotEngine;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Anonymous Ballot Hashing Engine Unit Tests")
class AnonymousBallotEngineTest {

    private final AnonymousBallotEngine engine = new AnonymousBallotEngine();

    @Test
    @DisplayName("Should produce deterministic voter nullifier for identical inputs")
    void shouldProduceDeterministicNullifier() {
        String token1 = engine.generateVoterNullifier(10L, 55L, "secret-salt-xyz");
        String token2 = engine.generateVoterNullifier(10L, 55L, "secret-salt-xyz");

        assertThat(token1).isEqualTo(token2);
        assertThat(token1).hasSize(64); // SHA-256 hex string
    }

    @Test
    @DisplayName("Should produce distinct nullifiers for different users on the same poll")
    void shouldProduceDistinctNullifiersForDifferentUsers() {
        String tokenUser1 = engine.generateVoterNullifier(10L, 55L, "secret-salt-xyz");
        String tokenUser2 = engine.generateVoterNullifier(10L, 56L, "secret-salt-xyz");

        assertThat(tokenUser1).isNotEqualTo(tokenUser2);
    }

    @Test
    @DisplayName("Should generate verifiable receipt token starting with RCPT-")
    void shouldGenerateReceiptToken() {
        String nullifier = engine.generateVoterNullifier(10L, 55L, "secret-salt-xyz");
        String receipt = engine.generateBallotReceipt(10L, nullifier, 1L);

        assertThat(receipt).startsWith("RCPT-");
        assertThat(receipt.length()).isGreaterThan(10);
    }
}