package com.manacommunity.api.insurance.partner;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;

/**
 * Verifies partner webhook signatures. Header format: {@code t=<epoch-seconds>,v1=<hex HMAC-SHA256>} where
 * the MAC covers {@code t + "." + rawBody}. Constant-time compare, replay window, fail-closed when no secret.
 */
public final class WebhookSignatureVerifier {

    public enum Result { VALID, MISSING_SECRET, MALFORMED, STALE, MISMATCH }

    private WebhookSignatureVerifier() {}

    public static Result verify(String secret, String header, byte[] body, long nowEpochSeconds, long toleranceSeconds) {
        if (secret == null || secret.isBlank()) return Result.MISSING_SECRET;
        if (header == null || header.isBlank()) return Result.MALFORMED;
        String t = null;
        String v1 = null;
        for (String part : header.split(",")) {
            String p = part.trim();
            if (p.startsWith("t=")) t = p.substring(2);
            else if (p.startsWith("v1=")) v1 = p.substring(3);
        }
        if (t == null || v1 == null) return Result.MALFORMED;
        long ts;
        try {
            ts = Long.parseLong(t);
        } catch (NumberFormatException e) {
            return Result.MALFORMED;
        }
        byte[] provided;
        try {
            provided = HexFormat.of().parseHex(v1);
        } catch (IllegalArgumentException e) {
            return Result.MALFORMED;
        }
        byte[] expected = hmac(secret, t, body == null ? new byte[0] : body);
        // Signature first (constant time), then freshness: avoids leaking which check failed via timing.
        boolean sigOk = MessageDigest.isEqual(expected, provided);
        boolean fresh = Math.abs(nowEpochSeconds - ts) <= toleranceSeconds;
        if (!sigOk) return Result.MISMATCH;
        return fresh ? Result.VALID : Result.STALE;
    }

    /** Helper for tests and for partners' sandbox tooling. */
    public static String sign(String secret, long ts, byte[] body) {
        return "t=" + ts + ",v1=" + HexFormat.of().formatHex(hmac(secret, String.valueOf(ts), body));
    }

    private static byte[] hmac(String secret, String t, byte[] body) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            mac.update((t + ".").getBytes(StandardCharsets.UTF_8));
            return mac.doFinal(body);
        } catch (Exception e) {
            throw new IllegalStateException("HMAC unavailable", e);
        }
    }
}
