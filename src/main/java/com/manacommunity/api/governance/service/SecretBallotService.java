package com.manacommunity.api.governance.service;

import com.manacommunity.api.governance.entity.CommitteeElection;
import com.manacommunity.api.governance.entity.ElectionSecretBallot;
import com.manacommunity.api.governance.entity.ElectionVoterRoll;
import com.manacommunity.api.governance.repository.CommitteeElectionRepository;
import com.manacommunity.api.governance.repository.ElectionNominationRepository;
import com.manacommunity.api.governance.repository.ElectionSecretBallotRepository;
import com.manacommunity.api.governance.repository.ElectionVoterRollRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.spec.RSAPrivateKeySpec;
import java.security.spec.RSAPublicKeySpec;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Chaumian RSA Blind-Signature Secret Ballot Engine.
 * Decouples ElectionVoterRoll (flatId, userId -> claimed token) from ElectionSecretBallots
 * (nullifier, nominationId, hash chain) so no admin or DB query can ever link a resident to a vote.
 */
@Service
@RequiredArgsConstructor
public class SecretBallotService {

    private final CommitteeElectionRepository electionRepo;
    private final ElectionNominationRepository nominationRepo;
    private final ElectionVoterRollRepository voterRollRepo;
    private final ElectionSecretBallotRepository secretBallotRepo;
    private final VotingEligibilityService eligibilityService;

    @Transactional
    public Map<String, Object> getElectionPublicKey(Long electionId) {
        CommitteeElection election = ensureRsaKeyPair(electionId);
        return Map.of(
                "electionId", election.getId(),
                "modulusHex", election.getRsaPublicModulus(),
                "publicExponentHex", election.getRsaPublicExponent()
        );
    }

    @Transactional
    public Map<String, Object> issueBlindToken(Long electionId, Long flatId, Long userId, String blindedMessageHex) {
        if (blindedMessageHex == null || blindedMessageHex.isBlank()) {
            throw new IllegalArgumentException("blindedMessageHex is required");
        }
        CommitteeElection election = ensureRsaKeyPair(electionId);
        if (!"VOTING_OPEN".equalsIgnoreCase(election.getStatus())) {
            throw new IllegalStateException("Voting is not open for election " + electionId);
        }

        VotingEligibilityService.EligibilityResult eligibility = eligibilityService.evaluate(
                election.getCommunityId(), flatId, userId,
                VotingEligibilityService.MODE_ONE_FLAT_ONE_VOTE, 90, true, null);
        if (!eligibility.eligible()) {
            throw new IllegalStateException(eligibility.message());
        }

        if (voterRollRepo.findByElectionIdAndFlatId(electionId, flatId).isPresent()) {
            throw new IllegalStateException("This flat has already claimed its election ballot token");
        }

        BigInteger n = new BigInteger(election.getRsaPublicModulus(), 16);
        BigInteger d = new BigInteger(election.getRsaPrivateExponent(), 16);
        BigInteger mPrime = new BigInteger(blindedMessageHex.trim(), 16);
        if (mPrime.signum() <= 0 || mPrime.compareTo(n) >= 0) {
            throw new IllegalArgumentException("Blinded message out of range for RSA modulus");
        }

        BigInteger sPrime = mPrime.modPow(d, n);
        String tokenHash = sha256(blindedMessageHex.trim());

        voterRollRepo.save(ElectionVoterRoll.builder()
                .electionId(electionId)
                .flatId(flatId)
                .userId(userId)
                .blindedTokenHash(tokenHash)
                .build());

        return Map.of(
                "electionId", electionId,
                "blindSignatureHex", sPrime.toString(16),
                "publicModulusHex", election.getRsaPublicModulus(),
                "publicExponentHex", election.getRsaPublicExponent(),
                "blindedTokenHash", tokenHash
        );
    }

    @Transactional
    public Map<String, Object> castSecretBallot(Long electionId, Long nominationId, String tokenHex, String unblindedSignatureHex) {
        if (tokenHex == null || tokenHex.isBlank() || unblindedSignatureHex == null || unblindedSignatureHex.isBlank()) {
            throw new IllegalArgumentException("tokenHex and unblindedSignatureHex are required");
        }
        CommitteeElection election = ensureRsaKeyPair(electionId);
        if (!"VOTING_OPEN".equalsIgnoreCase(election.getStatus())) {
            throw new IllegalStateException("Voting is not open for election " + electionId);
        }
        boolean validNomination = nominationRepo.findByElectionId(electionId).stream()
                .anyMatch(n -> n.getId().equals(nominationId));
        if (!validNomination) {
            throw new IllegalArgumentException("Invalid nomination " + nominationId + " for election " + electionId);
        }

        BigInteger n = new BigInteger(election.getRsaPublicModulus(), 16);
        BigInteger e = new BigInteger(election.getRsaPublicExponent(), 16);
        BigInteger s = new BigInteger(unblindedSignatureHex.trim(), 16);
        BigInteger expectedHashInt = tokenToMessageInt(tokenHex.trim(), n);

        if (!s.modPow(e, n).equals(expectedHashInt)) {
            throw new IllegalArgumentException("Invalid blind signature for secret ballot token");
        }

        String nullifier = sha256(tokenHex.trim());
        if (secretBallotRepo.findByBallotNullifier(nullifier).isPresent()) {
            throw new IllegalStateException("Duplicate secret ballot rejected");
        }

        String prevHash = (election.getBallotChainHead() != null && !election.getBallotChainHead().isBlank())
                ? election.getBallotChainHead()
                : "GENESIS";
        String ballotHash = sha256(prevHash + ":" + electionId + ":" + nullifier + ":" + nominationId);
        String receiptCode = "SB-" + sha256(tokenHex.trim() + ":" + ballotHash).substring(0, 14).toUpperCase();

        secretBallotRepo.save(ElectionSecretBallot.builder()
                .electionId(electionId)
                .nominationId(nominationId)
                .ballotNullifier(nullifier)
                .blindSignature(unblindedSignatureHex.trim())
                .prevBallotHash(prevHash)
                .ballotHash(ballotHash)
                .receiptCode(receiptCode)
                .build());

        election.setBallotChainHead(ballotHash);
        electionRepo.save(election);

        return Map.of(
                "accepted", true,
                "electionId", electionId,
                "receiptCode", receiptCode,
                "ballotHash", ballotHash
        );
    }

    @Transactional(readOnly = true)
    public Map<String, Object> verifyBallotAuditChain(Long electionId, String receiptCode) {
        long tokensIssued = voterRollRepo.countByElectionId(electionId);
        List<ElectionSecretBallot> ballots = secretBallotRepo.findByElectionIdOrderByIdAsc(electionId);

        String expectedPrev = "GENESIS";
        boolean chainValid = true;
        for (ElectionSecretBallot b : ballots) {
            if (!expectedPrev.equals(b.getPrevBallotHash())) {
                chainValid = false;
                break;
            }
            String recomputed = sha256(expectedPrev + ":" + electionId + ":" + b.getBallotNullifier() + ":" + b.getNominationId());
            if (!recomputed.equals(b.getBallotHash())) {
                chainValid = false;
                break;
            }
            expectedPrev = b.getBallotHash();
        }

        Map<Long, Long> tally = new LinkedHashMap<>();
        for (Object[] row : secretBallotRepo.countSecretVotesByNomination(electionId)) {
            tally.put((Long) row[0], (Long) row[1]);
        }

        boolean zeroStuffing = tokensIssued >= ballots.size();
        Map<String, Object> audit = new LinkedHashMap<>();
        audit.put("electionId", electionId);
        audit.put("tokensIssued", tokensIssued);
        audit.put("ballotsCast", ballots.size());
        audit.put("zeroStuffingVerified", zeroStuffing);
        audit.put("chainValid", chainValid && zeroStuffing);
        audit.put("chainHeadHash", expectedPrev);
        audit.put("talliesByNomination", tally);

        if (receiptCode != null && !receiptCode.isBlank()) {
            audit.put("receiptIncluded",
                    secretBallotRepo.findByElectionIdAndReceiptCode(electionId, receiptCode.trim()).isPresent());
        }
        return audit;
    }

    public static BigInteger tokenToMessageInt(String tokenHex, BigInteger modulus) {
        byte[] digest = sha256Bytes(tokenHex);
        return new BigInteger(1, digest).mod(modulus);
    }

    private CommitteeElection ensureRsaKeyPair(Long electionId) {
        CommitteeElection election = electionRepo.findById(electionId)
                .orElseThrow(() -> new IllegalArgumentException("Election not found: " + electionId));
        if (election.getRsaPublicModulus() == null || election.getRsaPrivateExponent() == null) {
            try {
                KeyPairGenerator gen = KeyPairGenerator.getInstance("RSA");
                gen.initialize(2048);
                KeyPair kp = gen.generateKeyPair();
                KeyFactory kf = KeyFactory.getInstance("RSA");
                RSAPublicKeySpec pub = kf.getKeySpec(kp.getPublic(), RSAPublicKeySpec.class);
                RSAPrivateKeySpec priv = kf.getKeySpec(kp.getPrivate(), RSAPrivateKeySpec.class);
                election.setRsaPublicModulus(pub.getModulus().toString(16));
                election.setRsaPublicExponent(pub.getPublicExponent().toString(16));
                election.setRsaPrivateExponent(priv.getPrivateExponent().toString(16));
                election.setBallotChainHead("GENESIS");
                election = electionRepo.save(election);
            } catch (Exception ex) {
                throw new RuntimeException("Failed to initialize RSA blind signature keys", ex);
            }
        }
        return election;
    }

    private String sha256(String input) {
        return HexFormat.of().formatHex(sha256Bytes(input));
    }

    private static byte[] sha256Bytes(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            return md.digest(input.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException ex) {
            throw new RuntimeException("SHA-256 unavailable", ex);
        }
    }
}
