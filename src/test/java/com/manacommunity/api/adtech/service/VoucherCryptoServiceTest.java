package com.manacommunity.api.adtech.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.manacommunity.api.adtech.dto.VoucherDtos.VoucherTokenPayloadDto;
import com.manacommunity.api.security.FieldEncryptionService;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;

class VoucherCryptoServiceTest {

    private final MockEnvironment devEnv = new MockEnvironment().withProperty("spring.profiles.active", "test");
    private final FieldEncryptionService encryptionService = new FieldEncryptionService("", devEnv);
    private final VoucherCryptoService cryptoService = new VoucherCryptoService(encryptionService, new ObjectMapper());

    @Test
    void matchesRfc6238TestVectorAndClockSkew() {
        String secretB64u = Base64.getUrlEncoder().withoutPadding()
                .encodeToString("12345678901234567890".getBytes(StandardCharsets.US_ASCII));

        assertEquals("287082", cryptoService.generateTotp(secretB64u, 59L, 6, 30));
        assertTrue(cryptoService.verifyTotp(secretB64u, "287082", 59L));
        assertTrue(cryptoService.verifyTotp(secretB64u, "287082", 89L)); // +1 window skew
        assertFalse(cryptoService.verifyTotp(secretB64u, "287082", 150L)); // stale
    }

    @Test
    void signsAndVerifiesQrPayloadAndRejectsTampering() {
        String hmacKey = cryptoService.generateRandomSecretBase64Url(32);
        String totpSecret = cryptoService.generateRandomSecretBase64Url(20);

        VoucherTokenPayloadDto payload = new VoucherTokenPayloadDto(
                1, "vch_123", "res_1", "camp_001", "soc_hyd_001", 1732730000L, 4000000000L, "AbCd1234"
        );
        String payloadB64 = cryptoService.encodePayload(payload);
        String hmacSuffix = cryptoService.signPayload(payloadB64, hmacKey);
        String otp = cryptoService.generateTotp(totpSecret, 1732730010L, 6, 30);

        String qr = VoucherCryptoService.QR_PREFIX + payloadB64 + "." + hmacSuffix + "." + otp;
        VoucherCryptoService.ParsedQr parsed = cryptoService.parseQrString(qr);

        assertNotNull(parsed);
        assertTrue(cryptoService.verifyPayloadSignature(parsed.payloadB64Url(), parsed.hmacSuffix(), hmacKey));
        assertFalse(cryptoService.verifyPayloadSignature(parsed.payloadB64Url() + "x", parsed.hmacSuffix(), hmacKey));
        assertTrue(cryptoService.verifyTotp(totpSecret, parsed.totpCode(), 1732730010L));
    }
}
