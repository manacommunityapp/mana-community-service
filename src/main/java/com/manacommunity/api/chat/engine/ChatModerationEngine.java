package com.manacommunity.api.chat.engine;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.regex.Pattern;

@Component
public class ChatModerationEngine {

    // Phone number pattern (10-12 digits with optional spaces/dashes)
    private static final Pattern PHONE_PATTERN = Pattern.compile("\\b(\\+?\\d{1,3}[- .]?)?\\(?\\d{3}\\)?[- .]?\\d{3}[- .]?\\d{4}\\b");
    
    // Credit card / 16 digit number pattern
    private static final Pattern CC_PATTERN = Pattern.compile("\\b(?:\\d{4}[ -]?){3}\\d{4}\\b");

    private static final List<String> BANNED_KEYWORDS = List.of(
            "abuse", "scam", "fraud", "hacked", "illegal"
    );

    /**
     * Moderates text and sanitizes PII (phone numbers, card numbers) by replacing with asterisks.
     */
    public String maskSensitiveInfo(String content) {
        if (content == null) return null;
        String sanitized = PHONE_PATTERN.matcher(content).replaceAll("[PHONE MASKED]");
        sanitized = CC_PATTERN.matcher(sanitized).replaceAll("[CARD MASKED]");
        return sanitized;
    }

    /**
     * Checks if content contains offensive/banned keywords.
     */
    public boolean containsInappropriateContent(String content) {
        if (content == null || content.isBlank()) return false;
        String lower = content.toLowerCase();
        for (String word : BANNED_KEYWORDS) {
            if (lower.contains(word)) return true;
        }
        return false;
    }

    /**
     * Detects potential spam/flooding based on repeat messages within threshold.
     */
    public boolean isSpamMessage(String currentMessage, String lastMessage, long secondsSinceLastMessage) {
        if (currentMessage == null) return false;
        if (secondsSinceLastMessage < 1) {
            return true; // Too rapid (< 1s)
        }
        if (secondsSinceLastMessage < 5 && currentMessage.equalsIgnoreCase(lastMessage)) {
            return true; // Duplicate message within 5s
        }
        return false;
    }
}
