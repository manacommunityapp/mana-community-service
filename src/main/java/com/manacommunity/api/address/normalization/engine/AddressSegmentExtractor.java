package com.manacommunity.api.address.normalization.engine;

import com.manacommunity.api.address.normalization.model.AddressParseResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Segmentation engine extracting flat, tower/block, pincode, city, and society name query.
 */
@Component
@RequiredArgsConstructor
public class AddressSegmentExtractor {

    private final AddressLexicalNormalizer lexicalNormalizer;

    // Pattern for 6-digit Indian PIN codes
    private static final Pattern PINCODE_PATTERN = Pattern.compile("\\b([1-9][0-9]{5})\\b");

    // Pattern for flat / unit numbers: "flat 402", "unit 12b", "402", "b-402"
    private static final Pattern FLAT_KEYWORD_PATTERN = Pattern.compile("(?i)\\bflat\\s+([a-z0-9-]+)\\b");
    private static final Pattern FLAT_ISOLATED_PATTERN = Pattern.compile("(?i)\\b([a-z]-\\d{1,4}|\\d{1,4}[a-z]?)\\b");

    // Pattern for tower / block: "block b", "tower 3", "tower b", "wing a"
    private static final Pattern TOWER_BLOCK_PATTERN = Pattern.compile("(?i)\\b(block|tower|wing)\\s+([a-z0-9]+)\\b");

    // Major metropolitan city tokens in India
    private static final Pattern CITY_PATTERN = Pattern.compile("(?i)\\b(hyderabad|bengaluru|mumbai|pune|chennai|delhi|noida|gurugram|kolkata)\\b");

    /**
     * Parse unstructured string into structured AddressParseResult.
     */
    public AddressParseResult extractSegments(String rawAddress, String cityHint, String pincodeHint) {
        String normalized = lexicalNormalizer.normalize(rawAddress);

        // 1. Extract PIN code
        String pincode = pincodeHint;
        Matcher pinMatcher = PINCODE_PATTERN.matcher(normalized);
        if (pinMatcher.find()) {
            pincode = pinMatcher.group(1);
        }

        // 2. Extract Tower / Block
        String towerOrBlock = null;
        Matcher towerMatcher = TOWER_BLOCK_PATTERN.matcher(normalized);
        if (towerMatcher.find()) {
            towerOrBlock = towerMatcher.group(1) + " " + towerMatcher.group(2);
        }

        // 3. Extract Flat Number
        String flatNumber = null;
        Matcher flatKeywordMatcher = FLAT_KEYWORD_PATTERN.matcher(normalized);
        if (flatKeywordMatcher.find()) {
            flatNumber = flatKeywordMatcher.group(1).toUpperCase();
        } else {
            // Check for isolated flat token like "402" or "B-402"
            Matcher flatIsoMatcher = FLAT_ISOLATED_PATTERN.matcher(normalized);
            while (flatIsoMatcher.find()) {
                String candidate = flatIsoMatcher.group(1);
                // Avoid capturing 6-digit pincode prefix
                if (candidate.length() <= 5 && !candidate.equals(pincode)) {
                    flatNumber = candidate.toUpperCase();
                    break;
                }
            }
        }

        // If tower/block was not explicitly prefixed with block/tower/wing, infer from hyphenated flat like "B-402"
        if (towerOrBlock == null && flatNumber != null && flatNumber.contains("-")) {
            String prefix = flatNumber.substring(0, flatNumber.indexOf('-')).trim();
            if (prefix.matches("^[A-Za-z0-9]{1,3}$")) {
                towerOrBlock = "Block " + prefix.toUpperCase();
            }
        }

        // 4. Extract City
        String city = cityHint;
        Matcher cityMatcher = CITY_PATTERN.matcher(normalized);
        if (cityMatcher.find()) {
            city = cityMatcher.group(1);
        }

        // 5. Residual text extraction for Society Name Query
        String residual = normalized;
        if (pincode != null) residual = residual.replace(pincode, "");
        if (towerOrBlock != null) residual = residual.replace(towerOrBlock, "");
        if (flatNumber != null) {
            residual = residual.replace("flat " + flatNumber.toLowerCase(), "");
            residual = residual.replace(flatNumber.toLowerCase(), "");
        }
        if (city != null) residual = residual.replace(city.toLowerCase(), "");

        // Clean up common lingering words
        residual = residual.replaceAll("(?i)\\bflat\\b|\\bblock\\b|\\btower\\b|\\bwing\\b", "")
                .replaceAll("\\s+", " ")
                .trim();

        // Locality guess: if residual contains known locality words or trailing segment
        String locality = "";
        String societyQuery = residual;
        if (residual.contains("nallagandla")) {
            locality = "nallagandla";
            societyQuery = residual.replace("nallagandla", "").trim();
        } else if (residual.contains("whitefield")) {
            locality = "whitefield";
            societyQuery = residual.replace("whitefield", "").trim();
        } else if (residual.contains("hitech city") || residual.contains("hitec city")) {
            locality = "hitec city";
            societyQuery = residual.replaceAll("(?i)hitec city|hitech city", "").trim();
        }

        return new AddressParseResult(
                flatNumber,
                towerOrBlock != null ? capitalize(towerOrBlock) : null,
                capitalize(societyQuery),
                capitalize(locality),
                capitalize(city),
                pincode,
                normalized
        );
    }

    private String capitalize(String text) {
        if (text == null || text.isBlank()) return text;
        String[] words = text.split("\\s+");
        StringBuilder sb = new StringBuilder();
        for (String w : words) {
            if (!w.isBlank()) {
                sb.append(Character.toUpperCase(w.charAt(0)))
                  .append(w.substring(1).toLowerCase())
                  .append(" ");
            }
        }
        return sb.toString().trim();
    }
}
