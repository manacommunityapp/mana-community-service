package com.manacommunity.api.security;

import com.manacommunity.api.exception.ManaCommunityException;
import org.springframework.http.HttpStatus;

import java.util.List;

/**
 * Centralised password policy rules.
 *
 * <p>Requirements: 4 to 8 characters with a combination of alphabets and numbers.</p>
 */
public final class PasswordPolicy {

    public static final int MIN_LENGTH = 4;
    public static final int MAX_LENGTH = 8;

    private PasswordPolicy() {
    }

    /**
     * Throws {@link ManaCommunityException} (400 WEAK_PASSWORD) with a clear, UI-ready
     * message when {@code raw} fails length or alphanumeric combination requirements.
     */
    public static void validate(String raw) {
        validate(raw, List.of());
    }

    /**
     * Validates password meets 4–8 characters length and contains both alphabets and numbers.
     */
    public static void validate(String raw, List<String> userInputs) {
        if (raw == null || raw.isBlank()) {
            throw weak("Password is required.");
        }
        if (raw.length() < MIN_LENGTH || raw.length() > MAX_LENGTH) {
            throw weak("Password must be between " + MIN_LENGTH + " and " + MAX_LENGTH + " characters long.");
        }
        boolean hasLetter = false, hasDigit = false;
        for (int i = 0; i < raw.length(); i++) {
            char c = raw.charAt(i);
            if (Character.isLetter(c)) hasLetter = true;
            else if (Character.isDigit(c)) hasDigit = true;
        }
        if (!hasLetter || !hasDigit) {
            throw weak("Password must contain a combination of letters and numbers.");
        }
    }

    /** Non-blocking advisory label ("weak"/"fair"/"good") for UI display. */
    public static String strengthHint(String raw) {
        if (raw == null || raw.length() < MIN_LENGTH) return "weak";
        boolean hasLetter = false, hasDigit = false;
        for (int i = 0; i < raw.length(); i++) {
            char c = raw.charAt(i);
            if (Character.isLetter(c)) hasLetter = true;
            else if (Character.isDigit(c)) hasDigit = true;
        }
        if (hasLetter && hasDigit) return "good";
        return "fair";
    }

    private static ManaCommunityException weak(String message) {
        return new ManaCommunityException(message, HttpStatus.BAD_REQUEST, "WEAK_PASSWORD");
    }
}
