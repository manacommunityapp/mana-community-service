package com.manacommunity.api.config;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.EnumerablePropertySource;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.MutablePropertySources;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.spec.KeySpec;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Decrypts any property value wrapped in {@code ENC(...)} at startup.
 *
 * <p>The master password is read from the {@code JASYPT_ENCRYPTOR_PASSWORD} env var
 * (or JVM system property {@code jasypt.encryptor.password}). If neither is set,
 * ENC() values are left as-is and a warning is printed — the datasource will fail
 * with the encrypted gibberish, which is safer than silently connecting with no password.</p>
 *
 * <h3>How to encrypt a value</h3>
 * <pre>
 *   # On any machine with Java installed:
 *   java -cp manacommunity.jar com.manacommunity.api.config.EncryptablePropertyProcessor \
 *        encrypt "your-master-password" "plaintext-to-encrypt"
 *
 *   # Output:  ENC(base64string)
 *   # Paste that into /etc/mana-community/env:
 *   #   DB_PASSWORD=ENC(base64string)
 * </pre>
 *
 * <p>Algorithm: PBKDF2WithHmacSHA256 key derivation + AES-256-GCM. The salt and IV
 * are prepended to the ciphertext before Base64 encoding.</p>
 */
public class EncryptablePropertyProcessor implements EnvironmentPostProcessor {

    private static final String ENC_PREFIX = "ENC(";
    private static final String ENC_SUFFIX = ")";
    private static final int SALT_LENGTH = 16;
    private static final int IV_LENGTH = 12;
    private static final int GCM_TAG_BITS = 128;
    private static final int KEY_LENGTH = 256;
    private static final int ITERATIONS = 65536;

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        String masterPassword = resolveMasterPassword(environment);
        if (masterPassword == null) return;

        Map<String, Object> decrypted = new LinkedHashMap<>();
        MutablePropertySources sources = environment.getPropertySources();

        for (org.springframework.core.env.PropertySource<?> ps : sources) {
            if (ps instanceof EnumerablePropertySource<?> eps) {
                for (String name : eps.getPropertyNames()) {
                    Object raw = eps.getProperty(name);
                    if (raw instanceof String s && s.startsWith(ENC_PREFIX) && s.endsWith(ENC_SUFFIX)) {
                        String cipher = s.substring(ENC_PREFIX.length(), s.length() - ENC_SUFFIX.length());
                        try {
                            decrypted.put(name, decrypt(cipher, masterPassword));
                        } catch (Exception e) {
                            System.err.println("[EncryptableProperty] Failed to decrypt property '"
                                    + name + "': " + e.getMessage());
                        }
                    }
                }
            }
        }

        if (!decrypted.isEmpty()) {
            sources.addFirst(new MapPropertySource("decryptedProperties", decrypted));
        }
    }

    private String resolveMasterPassword(ConfigurableEnvironment env) {
        String pw = env.getProperty("jasypt.encryptor.password");
        if (pw != null && !pw.isBlank()) return pw;
        pw = System.getenv("JASYPT_ENCRYPTOR_PASSWORD");
        if (pw != null && !pw.isBlank()) return pw;
        return null;
    }

    // ── Crypto helpers ──────────────────────────────────────────────────

    static String encrypt(String plaintext, String masterPassword) throws Exception {
        byte[] salt = new byte[SALT_LENGTH];
        byte[] iv = new byte[IV_LENGTH];
        java.security.SecureRandom sr = new java.security.SecureRandom();
        sr.nextBytes(salt);
        sr.nextBytes(iv);

        SecretKey key = deriveKey(masterPassword, salt);
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(GCM_TAG_BITS, iv));
        byte[] ciphertext = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));

        byte[] combined = new byte[SALT_LENGTH + IV_LENGTH + ciphertext.length];
        System.arraycopy(salt, 0, combined, 0, SALT_LENGTH);
        System.arraycopy(iv, 0, combined, SALT_LENGTH, IV_LENGTH);
        System.arraycopy(ciphertext, 0, combined, SALT_LENGTH + IV_LENGTH, ciphertext.length);

        return Base64.getEncoder().encodeToString(combined);
    }

    static String decrypt(String base64Cipher, String masterPassword) throws Exception {
        byte[] combined = Base64.getDecoder().decode(base64Cipher);
        if (combined.length < SALT_LENGTH + IV_LENGTH + 1) {
            throw new IllegalArgumentException("Ciphertext too short");
        }

        byte[] salt = new byte[SALT_LENGTH];
        byte[] iv = new byte[IV_LENGTH];
        byte[] ciphertext = new byte[combined.length - SALT_LENGTH - IV_LENGTH];
        System.arraycopy(combined, 0, salt, 0, SALT_LENGTH);
        System.arraycopy(combined, SALT_LENGTH, iv, 0, IV_LENGTH);
        System.arraycopy(combined, SALT_LENGTH + IV_LENGTH, ciphertext, 0, ciphertext.length);

        SecretKey key = deriveKey(masterPassword, salt);
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(GCM_TAG_BITS, iv));
        return new String(cipher.doFinal(ciphertext), StandardCharsets.UTF_8);
    }

    private static SecretKey deriveKey(String password, byte[] salt) throws Exception {
        KeySpec spec = new PBEKeySpec(password.toCharArray(), salt, ITERATIONS, KEY_LENGTH);
        SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
        byte[] keyBytes = factory.generateSecret(spec).getEncoded();
        return new SecretKeySpec(keyBytes, "AES");
    }

    // ── CLI tool for encrypting values ──────────────────────────────────

    public static void main(String[] args) throws Exception {
        if (args.length < 3) {
            System.out.println("Usage:");
            System.out.println("  encrypt <master-password> <plaintext>");
            System.out.println("  decrypt <master-password> <ENC(...)>");
            System.exit(1);
        }
        String action = args[0];
        String master = args[1];
        String value = args[2];

        if ("encrypt".equalsIgnoreCase(action)) {
            System.out.println(ENC_PREFIX + encrypt(value, master) + ENC_SUFFIX);
        } else if ("decrypt".equalsIgnoreCase(action)) {
            String cipher = value;
            if (cipher.startsWith(ENC_PREFIX) && cipher.endsWith(ENC_SUFFIX)) {
                cipher = cipher.substring(ENC_PREFIX.length(), cipher.length() - ENC_SUFFIX.length());
            }
            System.out.println(decrypt(cipher, master));
        } else {
            System.err.println("Unknown action: " + action + ". Use 'encrypt' or 'decrypt'.");
            System.exit(1);
        }
    }
}
