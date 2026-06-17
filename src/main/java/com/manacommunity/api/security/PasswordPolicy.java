package com.manacommunity.api.security;

import com.manacommunity.api.exception.ManaCommunityException;
import org.springframework.http.HttpStatus;

/**
 * Centralised password-strength rules (no server-side state).
 *
 * <p>Requirements enforced: minimum 8 characters and at least one each of
 * uppercase, lowercase, digit and special character. 12+ characters is
 * recommended; {@link #strengthHint(String)} reports it but does not block.</p>
 */
public final class PasswordPolicy {

    public static final int MIN_LENGTH = 8;
    public static final int RECOMMENDED_LENGTH = 12;
    private static final String SPECIALS = "!@#$%^&*()_+-=[]{}|;:',.<>?/~`\"\\";

    private PasswordPolicy() {
    }

    /**
     * Throws {@link ManaCommunityException} (400 WEAK_PASSWORD) with a clear, UI-ready
     * message when {@code raw} fails any hard requirement.
     */
    public static void validate(String raw) {
        if (raw == null || raw.isBlank()) {
            throw weak("Password is required.");
        }
        if (raw.length() < MIN_LENGTH) {
            throw weak("Password must be at least " + MIN_LENGTH + " characters long.");
        }
        boolean upper = false, lower = false, digit = false, special = false;
        for (int i = 0; i < raw.length(); i++) {
            char c = raw.charAt(i);
            if (Character.isUpperCase(c)) upper = true;
            else if (Character.isLowerCase(c)) lower = true;
            else if (Character.isDigit(c)) digit = true;
            else if (SPECIALS.indexOf(c) >= 0) special = true;
        }
        StringBuilder missing = new StringBuilder();
        if (!upper) missing.append("an uppercase letter, ");
        if (!lower) missing.append("a lowercase letter, ");
        if (!digit) missing.append("a number, ");
        if (!special) missing.append("a special character, ");
        if (missing.length() > 0) {
            missing.setLength(missing.length() - 2); // trim trailing ", "
            throw weak("Password must include " + missing + ".");
        }
    }

    /** Non-blocking advisory ("strong"/"good"/"acceptable") for UI display. */
    public static String strengthHint(String raw) {
        if (raw == null) return "weak";
        if (raw.length() >= RECOMMENDED_LENGTH) return "strong";
        if (raw.length() >= MIN_LENGTH) return "acceptable";
        return "weak";
    }

    private static ManaCommunityException weak(String message) {
        return new ManaCommunityException(message, HttpStatus.BAD_REQUEST, "WEAK_PASSWORD");
    }
}
