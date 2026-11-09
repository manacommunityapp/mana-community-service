package com.manacommunity.api.sports.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Shared formatting utilities for the sports module, ensuring consistent
 * block and flat address formatting (e.g. "Block B, Flat 1008") across
 * registrations, dashboard views, partner selectors, and exports.
 */
public final class SportsFormatUtils {

    private static final Pattern COMPLETE_BLOCK_FLAT = Pattern.compile(
            "(?i)^(?:block|tower|wing)\\s+[a-z0-9_-]+,\\s*flat\\s+[a-z0-9_-]+$");
    private static final Pattern HYPHENATED_BLOCK_FLAT = Pattern.compile(
            "(?i)^((?:(?:block|tower|wing)\\s+)?[a-z0-9]+)\\s*[-/]\\s*([a-z0-9]+)$");
    private static final Pattern SPACED_BLOCK_FLAT = Pattern.compile(
            "(?i)^((?:block|tower|wing)\\s+[a-z0-9]+)\\s+(?:flat\\s+)?([a-z0-9]+)$");
    private static final Pattern VILLA_OR_SPECIAL = Pattern.compile(
            "(?i)^(?:villa|house|penthouse|row\\s*house)\\b.*$");

    private SportsFormatUtils() {}

    /**
     * Formats block and flat into a clean, standard community address string.
     * Examples:
     * - ("Block B", "1008") -> "Block B, Flat 1008"
     * - ("B", "1008") -> "Block B, Flat 1008"
     * - ("Tower 2", "401") -> "Tower 2, Flat 401"
     * - (null, "B-1008") -> "Block B, Flat 1008"
     * - (null, "1008") -> "Flat 1008"
     * - (null, "Block B, Flat 1008") -> "Block B, Flat 1008"
     * - (null, "Villa 42") -> "Villa 42"
     */
    public static String formatBlockAndFlat(String block, String flat) {
        String f = flat != null ? flat.trim() : "";
        String b = block != null ? block.trim() : "";

        if (f.isEmpty() && b.isEmpty()) {
            return null;
        }

        // Already fully formatted e.g. "Block B, Flat 1008"
        if (COMPLETE_BLOCK_FLAT.matcher(f).matches()) {
            return f;
        }

        // Standalone villa/house
        if (VILLA_OR_SPECIAL.matcher(f).matches()) {
            return f;
        }

        // Hyphenated e.g. "B-1008" or "Tower 2-401"
        Matcher hyphenMatcher = HYPHENATED_BLOCK_FLAT.matcher(f);
        if (hyphenMatcher.matches()) {
            String extractedBlock = hyphenMatcher.group(1).trim();
            String extractedFlat = hyphenMatcher.group(2).trim();
            String cleanB = normalizeBlock(extractedBlock);
            return cleanB + ", Flat " + extractedFlat;
        }

        // Spaced e.g. "Block B 1008"
        Matcher spacedMatcher = SPACED_BLOCK_FLAT.matcher(f);
        if (spacedMatcher.matches()) {
            String extractedBlock = spacedMatcher.group(1).trim();
            String extractedFlat = spacedMatcher.group(2).trim();
            return extractedBlock + ", Flat " + extractedFlat;
        }

        // Clean leading "Flat " from f if present
        f = f.replaceFirst("(?i)^flat\\s+", "").trim();

        if (!b.isEmpty()) {
            String cleanB = normalizeBlock(b);
            if (f.isEmpty()) {
                return cleanB;
            }
            if (f.equalsIgnoreCase(cleanB) || f.toLowerCase().startsWith(cleanB.toLowerCase() + " ")) {
                return f;
            }
            if (f.equalsIgnoreCase(b) || f.toLowerCase().startsWith(b.toLowerCase() + " ")) {
                return cleanB + f.substring(b.length());
            }
            return cleanB + ", Flat " + f;
        }

        if (!f.isEmpty()) {
            return "Flat " + f;
        }

        return null;
    }

    private static String normalizeBlock(String b) {
        if (b == null || b.isBlank()) return "";
        String trimmed = b.trim();
        String lower = trimmed.toLowerCase();
        if (lower.startsWith("block") || lower.startsWith("tower") || lower.startsWith("wing")) {
            return trimmed;
        }
        return "Block " + trimmed;
    }
}
