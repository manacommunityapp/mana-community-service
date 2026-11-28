package com.manacommunity.api.forensics.service;

import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.spec.ECGenParameterSpec;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;

/**
 * Cryptographic primitives for bitemporal hash chains, binary Merkle tree roots,
 * and KMS-isolated asymmetric ECDSA signatures (SHA256withECDSA on P-256).
 */
public final class ForensicsCryptoUtil {

    static final String GENESIS_HASH = "0".repeat(64);
    private static final KeyPair KMS_ECDSA_KEYPAIR = generateEphemeralOrProcessKeyPair();

    private ForensicsCryptoUtil() {}

    private static KeyPair generateEphemeralOrProcessKeyPair() {
        try {
            KeyPairGenerator kpg = KeyPairGenerator.getInstance("EC");
            kpg.initialize(new ECGenParameterSpec("secp256r1"));
            return kpg.generateKeyPair();
        } catch (Exception e) {
            throw new IllegalStateException("ECDSA secp256r1 keypair initialization failed", e);
        }
    }

    public static String sha256Hex(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(md.digest((input == null ? "" : input).getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }

    public static String signEcdsa(String payload) {
        try {
            Signature sig = Signature.getInstance("SHA256withECDSA");
            sig.initSign(KMS_ECDSA_KEYPAIR.getPrivate());
            sig.update(payload.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(sig.sign());
        } catch (Exception e) {
            throw new IllegalStateException("ECDSA signing failed", e);
        }
    }

    public static boolean verifyEcdsa(String payload, String signatureB64) {
        if (payload == null || signatureB64 == null || signatureB64.isBlank()) {
            return false;
        }
        try {
            Signature sig = Signature.getInstance("SHA256withECDSA");
            sig.initVerify(KMS_ECDSA_KEYPAIR.getPublic());
            sig.update(payload.getBytes(StandardCharsets.UTF_8));
            return sig.verify(Base64.getUrlDecoder().decode(signatureB64));
        } catch (Exception e) {
            return false;
        }
    }

    public static String computeMerkleRoot(List<String> leafHashes) {
        if (leafHashes == null || leafHashes.isEmpty()) {
            return GENESIS_HASH;
        }
        List<String> layer = new ArrayList<>(leafHashes);
        while (layer.size() > 1) {
            List<String> next = new ArrayList<>((layer.size() + 1) / 2);
            for (int i = 0; i < layer.size(); i += 2) {
                String left = layer.get(i);
                String right = (i + 1 < layer.size()) ? layer.get(i + 1) : left;
                next.add(sha256Hex(left + right));
            }
            layer = next;
        }
        return layer.get(0);
    }
}
