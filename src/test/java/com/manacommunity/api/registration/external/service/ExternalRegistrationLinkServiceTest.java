package com.manacommunity.api.registration.external.service;

import com.manacommunity.api.registration.external.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ExternalRegistrationLinkServiceTest {

    private ExternalRegistrationLinkService linkService;

    @BeforeEach
    void setUp() {
        ExternalRegistrationLinkCryptoService cryptoService =
                new ExternalRegistrationLinkCryptoService("unit-test-secret-key-32-bytes-minimum-length!!");
        linkService = new ExternalRegistrationLinkService(cryptoService);
    }

    @Test
    void testGenerateLinkAndValidate() {
        GenerateRegistrationLinkRequest req = GenerateRegistrationLinkRequest.builder()
                .communityId(101L)
                .scope(RegistrationScope.TOURNAMENT_PLAYER)
                .referenceId(77L)
                .scopeTitle("Merilon Badminton Open")
                .maxUses(50)
                .expiresAt(Instant.now().plus(3, ChronoUnit.DAYS))
                .metadata(Map.of("court", "Court 1"))
                .build();

        GenerateRegistrationLinkResponse res = linkService.generateLink(req);
        assertNotNull(res);
        assertNotNull(res.signedToken());
        assertTrue(res.registrationUrl().contains("/register/external?token="));

        SignedRegistrationToken validated = linkService.validateLink(res.signedToken());
        assertEquals(101L, validated.communityId());
        assertEquals("Merilon Badminton Open", validated.scopeTitle());
        assertEquals(50, validated.maxUses());
    }

    @Test
    void testSubmitPublicRegistrationSuccess() {
        GenerateRegistrationLinkRequest linkReq = GenerateRegistrationLinkRequest.builder()
                .communityId(102L)
                .scope(RegistrationScope.TOURNAMENT_PLAYER)
                .referenceId(88L)
                .scopeTitle("Cricket League 2026")
                .maxUses(10)
                .expiresAt(Instant.now().plus(5, ChronoUnit.DAYS))
                .build();

        GenerateRegistrationLinkResponse linkRes = linkService.generateLink(linkReq);

        PublicRegistrationSubmissionRequest submitReq = PublicRegistrationSubmissionRequest.builder()
                .signedToken(linkRes.signedToken())
                .applicantName("Vikram Sharma")
                .phoneNumber("+919876543210")
                .email("vikram.sharma@example.com")
                .teamName("Royal Challengers")
                .playerRole("All-Rounder")
                .preferredJerseyNumber("18")
                .build();

        PublicRegistrationSubmissionResponse submitRes = linkService.submitRegistration(submitReq);
        assertNotNull(submitRes);
        assertEquals("CONFIRMED", submitRes.status());
        assertEquals("Vikram Sharma", submitRes.applicantName());
        assertTrue(submitRes.confirmationCode().startsWith("MC-"));
        assertTrue(submitRes.message().contains("Cricket League 2026"));
    }

    @Test
    void testCapacityEnforcementRejection() {
        GenerateRegistrationLinkRequest linkReq = GenerateRegistrationLinkRequest.builder()
                .communityId(103L)
                .scope(RegistrationScope.COMMUNITY_RESIDENT)
                .referenceId(1L)
                .scopeTitle("Limited Resident Batch")
                .maxUses(1) // Capacity is strictly 1
                .expiresAt(Instant.now().plus(1, ChronoUnit.DAYS))
                .build();

        GenerateRegistrationLinkResponse linkRes = linkService.generateLink(linkReq);

        // First registration succeeds
        PublicRegistrationSubmissionRequest submit1 = PublicRegistrationSubmissionRequest.builder()
                .signedToken(linkRes.signedToken())
                .applicantName("Resident 1")
                .phoneNumber("9000000001")
                .build();
        assertNotNull(linkService.submitRegistration(submit1));

        // Second registration fails due to capacity limit
        PublicRegistrationSubmissionRequest submit2 = PublicRegistrationSubmissionRequest.builder()
                .signedToken(linkRes.signedToken())
                .applicantName("Resident 2")
                .phoneNumber("9000000002")
                .build();

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> linkService.submitRegistration(submit2));
        assertTrue(ex.getMessage().contains("capacity limit reached"));
    }
}
