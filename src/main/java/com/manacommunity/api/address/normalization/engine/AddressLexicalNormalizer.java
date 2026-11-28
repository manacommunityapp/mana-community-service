package com.manacommunity.api.address.normalization.engine;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.regex.Pattern;

/**
 * Lexical pre-processor that strips noise, expands Indian real-estate abbreviations, and collapses whitespace.
 */
@Component
public class AddressLexicalNormalizer {

    private static final Pattern NOISE_CHARS = Pattern.compile("[,;:_#|/\t\r\n]+");
    private static final Pattern MULTI_SPACES = Pattern.compile("\\s+");

    // Standard abbreviation replacements for Indian delivery strings
    private static final Map<Pattern, String> REPLACEMENTS = Map.of(
            Pattern.compile("(?i)\\bblk\\b|(?i)\\bbk\\b"), "block",
            Pattern.compile("(?i)\\btwr\\b|(?i)\\btw\\b"), "tower",
            Pattern.compile("(?i)\\bwng\\b"), "wing",
            Pattern.compile("(?i)\\bfl\\b|(?i)\\bapt\\b|(?i)\\bapts\\b|(?i)\\bunit\\b"), "flat",
            Pattern.compile("(?i)\\bhyd\\b|(?i)\\bhydrabad\\b|(?i)\\bsecunderabad\\b"), "hyderabad",
            Pattern.compile("(?i)\\bblr\\b|(?i)\\bbangalore\\b"), "bengaluru",
            Pattern.compile("(?i)\\bopp\\b|(?i)\\bopposite\\b|(?i)\\bnear\\b|(?i)\\bbeside\\b"), ""
    );

    /**
     * Normalize unstructured input string into clean lowercase tokens.
     */
    public String normalize(String raw) {
        if (raw == null) return "";

        String text = raw.trim().toLowerCase();

        // Strip special punctuation characters while preserving hyphens in compound words (like B-402)
        text = NOISE_CHARS.matcher(text).replaceAll(" ");

        // Apply abbreviation expansions
        for (Map.Entry<Pattern, String> entry : REPLACEMENTS.entrySet()) {
            text = entry.getKey().matcher(text).replaceAll(entry.getValue());
        }

        // Collapse duplicate whitespace
        return MULTI_SPACES.matcher(text).replaceAll(" ").trim();
    }
}
