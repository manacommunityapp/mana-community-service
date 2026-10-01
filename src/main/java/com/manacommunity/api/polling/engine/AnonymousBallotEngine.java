package com.manacommunity.api.polling.engine;

import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.UUID;

@Component
public class AnonymousBallotEngine {

    /**
     * Generates a cryptographic voter pseudonym (nullifier token).
     * Ensures one-person-one-vote while preventing any linkage to user identity.
     */
    public String generateVoterNullifier(Long pollId, Long userId, String pollSecretKey) {
        if (pollId == null || userId == null || pollSecretKey == null) {
            throw new IllegalArgumentException("Parameters cannot be null for nullifier generation");
        }
        String raw = pollId + ":" + userId + ":" + pollSecretKey;
        return sha256(raw);
    }

    /**
     * Generates an immutable, client-verifiable receipt token for the voter.
     * The voter can use this receipt to verify that their ballot was included in the final tally.
     */
    public String generateBallotReceipt(Long pollId, String nullifierToken, Long optionId) {
        String raw = pollId + ":" + nullifierToken + ":" + optionId + ":" + System.nanoTime();
        return "RCPT-" + sha256(raw).substring(0, 16).toUpperCase();
    }

    private String sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm unavailable", e);
        }
    }
}