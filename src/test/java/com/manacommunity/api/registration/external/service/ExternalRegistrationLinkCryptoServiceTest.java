package com.manacommunity.api.registration.external.service;

import com.manacommunity.api.registration.external.model.RegistrationScope;
import com.manacommunity.api.registration.external.model.SignedRegistrationToken;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ExternalRegistrationLinkCryptoServiceTest {

    private ExternalRegistrationLinkCryptoService cryptoService;

    @BeforeEach
    void setUp() {
        cryptoService = new ExternalRegistrationLinkCryptoService("test-secret-key-for-hmac-sha256-signing-12345678");
    }

    @Test
    void testSignAndVerifyValidToken() {
        SignedRegistrationToken token = new SignedRegistrationToken(
                "TOKEN-101",
                1L,
                RegistrationScope.TOURNAMENT_PLAYER,
                55L,
                "Lakshmi's Emperia Premier League",
                100,
                Instant.now().plus(7, ChronoUnit.DAYS),
                Map.of("sport", "CRICKET", "fee", "500")
        );

        String signed = cryptoService.signToken(token);
        assertNotNull(signed);
        assertTrue(signed.contains("."));

        SignedRegistrationToken parsed = cryptoService.verifyAndParseToken(signed);
        assertNotNull(parsed);
        assertEquals("TOKEN-101", parsed.tokenId());
        assertEquals(1L, parsed.communityId());
        assertEquals(RegistrationScope.TOURNAMENT_PLAYER, parsed.scope());
        assertEquals(55L, parsed.referenceId());
        assertEquals("Lakshmi's Emperia Premier League", parsed.scopeTitle());
        assertEquals("CRICKET", parsed.metadata().get("sport"));
    }

    @Test
    void testTamperedPayloadThrowsSecurityException() {
        SignedRegistrationToken token = new SignedRegistrationToken(
                "TOKEN-102",
                1L,
                RegistrationScope.TOURNAMENT_PLAYER,
                55L,
                "Championship",
                50,
                Instant.now().plus(1, ChronoUnit.DAYS),
                Map.of()
        );

        String signed = cryptoService.signToken(token);
        String[] parts = signed.split("\\.");

        // Tamper with payload by mutating characters
        String tamperedPayload = parts[0].substring(0, parts[0].length() - 4) + "AAAA";
        String tamperedToken = tamperedPayload + "." + parts[1];

        assertThrows(SecurityException.class, () -> cryptoService.verifyAndParseToken(tamperedToken));
    }

    @Test
    void testExpiredTokenThrowsIllegalStateException() {
        SignedRegistrationToken token = new SignedRegistrationToken(
                "TOKEN-103",
                1L,
                RegistrationScope.CLUBHOUSE_EVENT_GUEST,
                99L,
                "Diwali Gala",
                200,
                Instant.now().minus(1, ChronoUnit.HOURS), // Already expired
                Map.of()
        );

        String signed = cryptoService.signToken(token);
        assertThrows(IllegalStateException.class, () -> cryptoService.verifyAndParseToken(signed));
    }
}
