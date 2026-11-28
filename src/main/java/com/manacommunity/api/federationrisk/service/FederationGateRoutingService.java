package com.manacommunity.api.federationrisk.service;

import com.manacommunity.api.federationrisk.entity.FederationSubSociety;
import com.manacommunity.api.federationrisk.entity.FederationTransitToken;
import com.manacommunity.api.federationrisk.repository.FederationSubSocietyRepository;
import com.manacommunity.api.federationrisk.repository.FederationTransitTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * Hierarchical Mega-Township Gate Pre-Warming Service:
 * Routes approved vehicles/visitors from shared Outer Federation gates to specific inner sub-society gates
 * using short-TTL transit tokens containing zero ledger/dues/resident PII.
 */
@Service
@RequiredArgsConstructor
public class FederationGateRoutingService {

    public static final int DEFAULT_TRANSIT_TTL_MINUTES = 10;

    private final FederationSubSocietyRepository subSocietyRepo;
    private final FederationTransitTokenRepository transitRepo;

    @Transactional
    public FederationSubSociety registerSubSociety(Long federationId, Long societyId, String phaseLabel) {
        return subSocietyRepo.findByFederationIdAndSocietyId(federationId, societyId)
                .orElseGet(() -> subSocietyRepo.save(FederationSubSociety.builder()
                        .federationId(federationId)
                        .societyId(societyId)
                        .phaseLabel(phaseLabel)
                        .outerGateEnabled(true)
                        .build()));
    }

    @Transactional
    public FederationTransitToken admitAtOuterGateAndPrewarmInner(
            Long federationId,
            Long targetSocietyId,
            String opaqueFlatLabel,
            String rawPlateOrPhone,
            int ttlMinutes,
            Instant nowOverride
    ) {
        FederationSubSociety sub = subSocietyRepo.findByFederationIdAndSocietyId(federationId, targetSocietyId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Society " + targetSocietyId + " is not enrolled in federation " + federationId));
        if (!sub.isOuterGateEnabled()) {
            throw new IllegalStateException("Outer gate transit disabled for sub-society " + targetSocietyId);
        }

        Instant now = nowOverride != null ? nowOverride : Instant.now();
        int effectiveTtl = ttlMinutes > 0 ? Math.min(ttlMinutes, 60) : DEFAULT_TRANSIT_TTL_MINUTES;

        return transitRepo.save(FederationTransitToken.builder()
                .transitId("TR-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase(Locale.ROOT))
                .federationId(federationId)
                .targetSocietyId(targetSocietyId)
                .opaqueFlatLabel(opaqueFlatLabel) // Strictly opaque label, never financial ledger/PII
                .identifierHash(sha256Hex(rawPlateOrPhone))
                .validUntil(now.plusSeconds(effectiveTtl * 60L))
                .build());
    }

    @Transactional(readOnly = true)
    public List<FederationTransitToken> listPrewarmedForInnerGate(Long innerSocietyId, Instant nowOverride) {
        Instant now = nowOverride != null ? nowOverride : Instant.now();
        return transitRepo.findByTargetSocietyIdAndConsumedAtIsNullAndValidUntilAfter(innerSocietyId, now);
    }

    @Transactional
    public FederationTransitToken consumeTransitAtInnerGate(String transitId, Long innerSocietyId, Instant nowOverride) {
        Instant now = nowOverride != null ? nowOverride : Instant.now();
        FederationTransitToken token = transitRepo.findByTransitId(transitId)
                .orElseThrow(() -> new IllegalArgumentException("Transit token not found: " + transitId));

        if (!token.getTargetSocietyId().equals(innerSocietyId)) {
            throw new CrossSocietyIsolationException(
                    "Sub-society " + innerSocietyId + " cannot consume transit token scoped to society " + token.getTargetSocietyId());
        }
        if (token.getConsumedAt() != null) {
            throw new IllegalStateException("Transit token already consumed at " + token.getConsumedAt());
        }
        if (now.isAfter(token.getValidUntil())) {
            throw new IllegalStateException("Transit token expired at " + token.getValidUntil());
        }

        token.setConsumedAt(now);
        return transitRepo.save(token);
    }

    private static String sha256Hex(String value) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            String norm = value != null ? value.trim().toUpperCase(Locale.ROOT) : "";
            return HexFormat.of().formatHex(md.digest(norm.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception ex) {
            throw new RuntimeException("SHA-256 failure", ex);
        }
    }

    public static class CrossSocietyIsolationException extends RuntimeException {
        public CrossSocietyIsolationException(String message) {
            super(message);
        }
    }
}
