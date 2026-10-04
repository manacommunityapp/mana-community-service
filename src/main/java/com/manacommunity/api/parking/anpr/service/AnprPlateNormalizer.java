package com.manacommunity.api.parking.anpr.service;

import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

/**
 * Normalises raw ANPR plate strings for consistent DB lookup.
 * Mirrors the normalisation logic in the Python plate_validator.py.
 */
@Component
public class AnprPlateNormalizer {

    private static final Pattern STRIP = Pattern.compile("[\\s\\-\\.]");

    /**
     * Strips spaces, hyphens and dots; uppercases the string.
     * e.g. "mh 12 ab 1234" -> "MH12AB1234"
     */
    public String normalize(String raw) {
        if (raw == null || raw.isBlank()) return "";
        return STRIP.matcher(raw.trim()).replaceAll("").toUpperCase();
    }

    /**
     * Returns true if two plates should be considered the same,
     * tolerating whitespace/case/hyphen differences.
     */
    public boolean matches(String plate1, String plate2) {
        return normalize(plate1).equals(normalize(plate2));
    }
}
