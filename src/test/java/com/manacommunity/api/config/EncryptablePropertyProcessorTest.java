package com.manacommunity.api.config;
import org.junit.jupiter.api.Test;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.StandardEnvironment;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class EncryptablePropertyProcessorTest {

    private static final String MASTER_KEY = "test-master-secret-key-12345";

    @Test
    void testEncryptDecryptRoundTrip() throws Exception {
        String plain = "manauser_super_secret";
        String cipher = EncryptablePropertyProcessor.encrypt(plain, MASTER_KEY);
        assertNotNull(cipher);

        String decrypted = EncryptablePropertyProcessor.decrypt(cipher, MASTER_KEY);
        assertEquals(plain, decrypted);
    }

    @Test
    void testPostProcessEnvironmentWithStandaloneEnc() throws Exception {
        String username = "manauser";
        String encryptedUser = "ENC(" + EncryptablePropertyProcessor.encrypt(username, MASTER_KEY) + ")";

        StandardEnvironment env = new StandardEnvironment();
        env.getSystemProperties().put("jasypt.encryptor.password", MASTER_KEY);

        Map<String, Object> props = new HashMap<>();
        props.put("spring.datasource.username", encryptedUser);
        env.getPropertySources().addLast(new MapPropertySource("testSource", props));

        EncryptablePropertyProcessor processor = new EncryptablePropertyProcessor();
        processor.postProcessEnvironment(env, null);

        assertEquals(username, env.getProperty("spring.datasource.username"));
    }

    @Test
    void testPostProcessEnvironmentWithEmbeddedPlaceholder() throws Exception {
        String username = "manauser";
        String encryptedUser = "ENC(" + EncryptablePropertyProcessor.encrypt(username, MASTER_KEY) + ")";

        StandardEnvironment env = new StandardEnvironment();
        env.getSystemProperties().put("jasypt.encryptor.password", MASTER_KEY);

        Map<String, Object> props = new HashMap<>();
        props.put("spring.datasource.username", "${DB_USERNAME:" + encryptedUser + "}");
        env.getPropertySources().addLast(new MapPropertySource("testSource", props));

        EncryptablePropertyProcessor processor = new EncryptablePropertyProcessor();
        processor.postProcessEnvironment(env, null);

        assertEquals(username, env.getProperty("spring.datasource.username"));

        env.getSystemProperties().put("DB_USERNAME", "custom_override_user");
        assertEquals("custom_override_user", env.getProperty("spring.datasource.username"));
    }
}
