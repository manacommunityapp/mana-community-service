package com.manacommunity.api.registration.external.service;

import com.manacommunity.api.registration.external.model.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Service orchestrating signed external registration link lifecycle, capacity enforcement,
 * and public submission verification.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ExternalRegistrationLinkService {

    private final ExternalRegistrationLinkCryptoService cryptoService;

    @Value("${mana.app.base-url:https://app.manacommunity.com}")
    private String appBaseUrl;

    // In-memory usage counter for active registration links: tokenId -> usedCount
    private final Map<String, AtomicInteger> linkUsageCounters = new ConcurrentHashMap<>();
    private final SecureRandom random = new SecureRandom();

    /**
     * Admin/Organizer generates a secure, signed registration link with an expiration and max-use limit.
     */
    public GenerateRegistrationLinkResponse generateLink(GenerateRegistrationLinkRequest req) {
        String tokenId = "REG-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        SignedRegistrationToken tokenPayload = new SignedRegistrationToken(
                tokenId,
                req.communityId(),
                req.scope(),
                req.referenceId(),
                req.scopeTitle() != null ? req.scopeTitle() : "Mana Community Event",
                req.maxUses() > 0 ? req.maxUses() : 1000,
                req.expiresAt(),
                req.metadata() != null ? req.metadata() : Map.of()
        );

        String signedToken = cryptoService.signToken(tokenPayload);
        String registrationUrl = appBaseUrl + "/register/external?token=" + signedToken;

        linkUsageCounters.put(tokenId, new AtomicInteger(0));

        log.info("[RegistrationLink] Generated link id={} scope={} refId={} expiresAt={}",
                tokenId, req.scope(), req.referenceId(), req.expiresAt());

        return GenerateRegistrationLinkResponse.builder()
                .tokenId(tokenId)
                .signedToken(signedToken)
                .registrationUrl(registrationUrl)
                .scope(req.scope())
                .referenceId(req.referenceId())
                .scopeTitle(tokenPayload.scopeTitle())
                .maxUses(tokenPayload.maxUses())
                .expiresAt(tokenPayload.expiresAt())
                .status("ACTIVE")
                .build();
    }

    /**
     * Validates a signed link when an applicant visits the public registration URL.
     */
    public SignedRegistrationToken validateLink(String signedToken) {
        SignedRegistrationToken token = cryptoService.verifyAndParseToken(signedToken);

        // Check if max capacity reached
        AtomicInteger counter = linkUsageCounters.get(token.tokenId());
        if (counter != null && counter.get() >= token.maxUses()) {
            throw new IllegalStateException("Registration capacity limit reached (" + token.maxUses() + " participants). Link is closed.");
        }

        return token;
    }

    /**
     * Submits a public registration from outside the app using a verified signed token.
     */
    public PublicRegistrationSubmissionResponse submitRegistration(PublicRegistrationSubmissionRequest req) {
        // 1. Verify token signature and validity
        SignedRegistrationToken token = validateLink(req.signedToken());

        // 2. Increment atomic usage count
        AtomicInteger counter = linkUsageCounters.computeIfAbsent(token.tokenId(), k -> new AtomicInteger(0));
        int currentCount = counter.incrementAndGet();
        if (currentCount > token.maxUses()) {
            counter.decrementAndGet();
            throw new IllegalStateException("Registration capacity limit reached (" + token.maxUses() + " participants).");
        }

        // 3. Generate confirmation code and registration record
        String confirmationCode = "MC-" + String.format("%06d", random.nextInt(1_000_000));
        String registrationId = "SUB-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        log.info("[RegistrationLink] Successful external registration id={} applicant='{}' scope={} refId={} confirmation={}",
                registrationId, req.applicantName(), token.scope(), token.referenceId(), confirmationCode);

        return PublicRegistrationSubmissionResponse.builder()
                .registrationId(registrationId)
                .status("CONFIRMED")
                .applicantName(req.applicantName())
                .scope(token.scope())
                .referenceId(token.referenceId())
                .scopeTitle(token.scopeTitle())
                .confirmationCode(confirmationCode)
                .registeredAt(Instant.now())
                .message("Successfully registered for " + token.scopeTitle() + ". Your confirmation code is: " + confirmationCode)
                .build();
    }
}
