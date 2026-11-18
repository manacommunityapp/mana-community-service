package com.manacommunity.api.unit.security;

import com.manacommunity.api.exception.ManaCommunityException;
import com.manacommunity.api.security.RsaKeyService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("RsaKeyService")
class RsaKeyServiceTest {

    private RsaKeyService rsaKeyService;

    @BeforeEach
    void setUp() {
        rsaKeyService = new RsaKeyService();
        rsaKeyService.init();
    }

    @Test
    @DisplayName("generates valid PEM public key and keyId on init")
    void generatesValidPublicKey() {
        assertThat(rsaKeyService.getPublicKeyPem()).isNotNull();
        assertThat(rsaKeyService.getPublicKeyPem()).startsWith("-----BEGIN PUBLIC KEY-----");
        assertThat(rsaKeyService.getPublicKeyPem()).endsWith("-----END PUBLIC KEY-----");
        assertThat(rsaKeyService.getKeyId()).isNotNull().startsWith("key-");
    }

    @Test
    @DisplayName("decrypts JSON payload containing password and fresh timestamp")
    void decryptsValidJsonPayload() throws Exception {
        long now = System.currentTimeMillis();
        String json = "{\"p\":\"MySecretPassword123\",\"t\":" + now + "}";
        byte[] cipherBytes = rsaKeyService.encryptForTesting(json.getBytes(StandardCharsets.UTF_8));
        String base64Cipher = Base64.getEncoder().encodeToString(cipherBytes);

        String decrypted = rsaKeyService.decryptPassword(base64Cipher);
        assertThat(decrypted).isEqualTo("MySecretPassword123");
    }

    @Test
    @DisplayName("rejects expired payload older than 5 minutes")
    void rejectsExpiredPayload() throws Exception {
        long sixMinutesAgo = System.currentTimeMillis() - (6 * 60 * 1000L);
        String json = "{\"p\":\"OldPassword\",\"t\":" + sixMinutesAgo + "}";
        byte[] cipherBytes = rsaKeyService.encryptForTesting(json.getBytes(StandardCharsets.UTF_8));
        String base64Cipher = Base64.getEncoder().encodeToString(cipherBytes);

        assertThatThrownBy(() -> rsaKeyService.decryptPassword(base64Cipher))
                .isInstanceOf(ManaCommunityException.class)
                .hasMessageContaining("expired");
    }

    @Test
    @DisplayName("throws on empty or blank payload")
    void throwsOnEmptyPayload() {
        assertThatThrownBy(() -> rsaKeyService.decryptPassword(""))
                .isInstanceOf(ManaCommunityException.class);
        assertThatThrownBy(() -> rsaKeyService.decryptPassword(null))
                .isInstanceOf(ManaCommunityException.class);
    }
}
