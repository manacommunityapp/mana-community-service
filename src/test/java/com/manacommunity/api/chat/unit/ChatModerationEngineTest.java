package com.manacommunity.api.chat.unit;

import com.manacommunity.api.chat.engine.ChatModerationEngine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Chat Moderation & Safety Engine Unit Tests")
class ChatModerationEngineTest {

    private ChatModerationEngine moderationEngine;

    @BeforeEach
    void setUp() {
        moderationEngine = new ChatModerationEngine();
    }

    @Test
    @DisplayName("Should mask phone numbers and credit card numbers")
    void shouldMaskSensitivePii() {
        String input = "Please call me on 9876543210 or card 4111-2222-3333-4444 for payment";
        String masked = moderationEngine.maskSensitiveInfo(input);

        assertFalse(masked.contains("9876543210"));
        assertFalse(masked.contains("4111-2222-3333-4444"));
        assertTrue(masked.contains("[PHONE MASKED]"));
        assertTrue(masked.contains("[CARD MASKED]"));
    }

    @Test
    @DisplayName("Should detect inappropriate keywords")
    void shouldDetectInappropriateContent() {
        assertTrue(moderationEngine.containsInappropriateContent("This looks like a fraud transaction"));
        assertFalse(moderationEngine.containsInappropriateContent("Hello everyone, good morning!"));
    }

    @Test
    @DisplayName("Should detect spam flood and duplicate messages")
    void shouldDetectSpam() {
        // rapid message within 0 seconds
        assertTrue(moderationEngine.isSpamMessage("Hello", "Hello", 0));
        // duplicate message within 3 seconds
        assertTrue(moderationEngine.isSpamMessage("Hello", "Hello", 3));
        // normal message after 10 seconds
        assertFalse(moderationEngine.isSpamMessage("Hello again", "Hello", 10));
    }
}
