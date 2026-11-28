package com.manacommunity.api.address.normalization.service;

import com.manacommunity.api.address.normalization.engine.AddressSegmentExtractor;
import com.manacommunity.api.address.normalization.engine.FuzzyStringMatcher;
import com.manacommunity.api.address.normalization.model.*;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * End-to-end address normalization and entity resolution service.
 * Resolves messy e-commerce delivery strings to canonical (community, tower, flat) database entities.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AddressEntityResolutionService {

    private final AddressSegmentExtractor segmentExtractor;
    private final FuzzyStringMatcher fuzzyMatcher;

    // Reference canonical society registry
    public record CanonicalSociety(
            Long communityId,
            String canonicalName,
            List<String> aliases,
            String locality,
            String city,
            String pincode,
            Map<String, Long> towerMap // "B" -> 12L, "A" -> 11L, "C" -> 13L
    ) {}

    private final List<CanonicalSociety> societyRegistry = new ArrayList<>();

    @PostConstruct
    public void initCanonicalRegistry() {
        societyRegistry.add(new CanonicalSociety(
                101L,
                "Aparna Sarovar Grande",
                List.of("Aparna Sarovar", "Aparna Sarowar", "Aparna Sarovar Grand", "ASG"),
                "Nallagandla",
                "Hyderabad",
                "500019",
                Map.of("A", 11L, "B", 12L, "C", 13L, "D", 14L, "E", 15L)
        ));

        societyRegistry.add(new CanonicalSociety(
                102L,
                "My Home Bhooja",
                List.of("MyHome Bhooja", "My Home Bhuja", "Bhooja"),
                "Hitech City",
                "Hyderabad",
                "500081",
                Map.of("A", 21L, "B", 22L, "C", 23L, "D", 24L, "J", 25L)
        ));

        societyRegistry.add(new CanonicalSociety(
                103L,
                "Prestige Shantiniketan",
                List.of("Prestige Shanti Niketan", "PSN", "Shantiniketan"),
                "Whitefield",
                "Bengaluru",
                "560048",
                Map.of("1", 31L, "2", 32L, "3", 33L, "A", 34L, "B", 35L)
        ));

        societyRegistry.add(new CanonicalSociety(
                104L,
                "Rainbow Vistas Rock Garden",
                List.of("Rainbow Vistas", "Rock Garden", "RVRG"),
                "Moosapet",
                "Hyderabad",
                "500018",
                Map.of("A", 41L, "B", 42L, "C", 43L, "SAPPHIRE", 44L)
        ));
    }

    /**
     * Normalize unstructured raw address and resolve to canonical records.
     */
    public AddressNormalizationResponse resolveAddress(AddressNormalizationRequest request) {
        long startTime = System.nanoTime();

        // Step 1 & 2: Lexical Normalization and Segment Extraction
        AddressParseResult parsed = segmentExtractor.extractSegments(
                request.rawAddress(),
                request.cityHint(),
                request.pincodeHint()
        );

        // Step 3: Multi-Signal Society Matching
        CanonicalSociety bestSociety = null;
        double bestSocietyScore = 0.0;

        for (CanonicalSociety society : societyRegistry) {
            double score = matchSociety(parsed, society);
            if (score > bestSocietyScore) {
                bestSocietyScore = score;
                bestSociety = society;
            }
        }

        double latencyMs = (System.nanoTime() - startTime) / 1_000_000.0;

        if (bestSociety == null || bestSocietyScore < 0.60) {
            return AddressNormalizationResponse.unresolved(
                    parsed,
                    latencyMs,
                    "No matching gated community found with sufficient string similarity (score: " + Math.round(bestSocietyScore * 100) / 100.0 + ")"
            );
        }

        // Step 4: Tower & Block Normalization
        Long towerId = null;
        String resolvedTowerName = null;
        double towerScore = 0.0;

        String towerCandidate = parsed.towerOrBlock();
        if (towerCandidate == null && parsed.flatNumber() != null && parsed.flatNumber().contains("-")) {
            String prefix = parsed.flatNumber().substring(0, parsed.flatNumber().indexOf('-')).trim();
            if (prefix.matches("^[A-Za-z0-9]{1,3}$")) {
                towerCandidate = prefix;
            }
        }

        if (bestSociety.towerMap().isEmpty()) {
            towerScore = 1.0;
        } else if (towerCandidate != null) {
            String extractedTowerKey = sanitizeTowerKey(towerCandidate);
            if (bestSociety.towerMap().containsKey(extractedTowerKey)) {
                towerId = bestSociety.towerMap().get(extractedTowerKey);
                resolvedTowerName = "Tower " + extractedTowerKey;
                towerScore = 1.0;
            } else {
                // Try fuzzy match on available towers
                for (Map.Entry<String, Long> entry : bestSociety.towerMap().entrySet()) {
                    double sim = fuzzyMatcher.computeSimilarity(extractedTowerKey, entry.getKey());
                    if (sim > 0.80 && sim > towerScore) {
                        towerScore = sim;
                        towerId = entry.getValue();
                        resolvedTowerName = "Tower " + entry.getKey();
                    }
                }
            }
        }

        // Step 5: Flat Number Verification
        double flatScore = 0.0;
        String resolvedFlatNumber = parsed.flatNumber();
        Long flatId = null;

        if (resolvedFlatNumber != null && !resolvedFlatNumber.isBlank()) {
            // Normalize flat number: "B-402" -> "402", "0402" -> "402"
            resolvedFlatNumber = sanitizeFlatNumber(resolvedFlatNumber);
            flatScore = 1.0;
            // Deterministic synthetic flat ID derived from tower and flat number
            flatId = (towerId != null ? towerId * 1000 : bestSociety.communityId() * 1000)
                    + parseFlatNumeric(resolvedFlatNumber);
        }

        // Step 6: Composite Confidence Arbitration
        double compositeConfidence = (0.50 * bestSocietyScore) + (0.30 * towerScore) + (0.20 * flatScore);
        compositeConfidence = Math.min(1.0, Math.round(compositeConfidence * 1000.0) / 1000.0);

        String status;
        String explanation;
        if (compositeConfidence >= 0.85) {
            status = "AUTO_RESOLVED";
            explanation = "High-confidence address match. Automated gate pass generation approved.";
        } else if (compositeConfidence >= 0.65) {
            status = "GUARD_DISAMBIGUATION_PROMPT";
            explanation = "Moderate confidence. Security guard tablet prompt required for tower/flat confirmation.";
        } else {
            status = "MANUAL_VERIFICATION_REQUIRED";
            explanation = "Low confidence. Manual security desk verification required.";
        }

        CanonicalAddressResolution canonical = new CanonicalAddressResolution(
                bestSociety.communityId(),
                bestSociety.canonicalName(),
                towerId,
                resolvedTowerName,
                flatId,
                resolvedFlatNumber
        );

        Map<String, Double> breakdown = Map.of(
                "societySimilarity", Math.round(bestSocietyScore * 1000.0) / 1000.0,
                "towerMatchScore", Math.round(towerScore * 1000.0) / 1000.0,
                "flatMatchScore", Math.round(flatScore * 1000.0) / 1000.0
        );

        return new AddressNormalizationResponse(
                status,
                compositeConfidence,
                parsed,
                canonical,
                breakdown,
                latencyMs,
                explanation
        );
    }

    private double matchSociety(AddressParseResult parsed, CanonicalSociety society) {
        String query = parsed.societyQuery();
        if (query == null || query.isBlank()) return 0.0;

        // 1. Direct canonical name similarity
        double maxScore = fuzzyMatcher.computeSimilarity(query, society.canonicalName());

        // 2. Check aliases (e.g. Aparna Sarowar, ASG, MyHome Bhooja)
        for (String alias : society.aliases()) {
            double aliasScore = fuzzyMatcher.computeSimilarity(query, alias);
            if (aliasScore > maxScore) {
                maxScore = aliasScore;
            }
        }

        // 3. Locality / City / Pincode boosting
        if (parsed.pincode() != null && parsed.pincode().equals(society.pincode())) {
            maxScore = Math.min(1.0, maxScore + 0.10);
        }
        if (parsed.city() != null && parsed.city().equalsIgnoreCase(society.city())) {
            maxScore = Math.min(1.0, maxScore + 0.05);
        }
        if (parsed.locality() != null && !parsed.locality().isBlank()
                && parsed.locality().equalsIgnoreCase(society.locality())) {
            maxScore = Math.min(1.0, maxScore + 0.08);
        }

        return Math.min(1.0, maxScore);
    }

    private String sanitizeTowerKey(String towerOrBlock) {
        return towerOrBlock.replaceAll("(?i)\\bblock\\b|\\btower\\b|\\bwing\\b", "")
                .trim()
                .toUpperCase();
    }

    private String sanitizeFlatNumber(String flat) {
        String s = flat.trim();
        // Remove tower prefix if attached: "B-402" -> "402"
        if (s.contains("-")) {
            s = s.substring(s.indexOf('-') + 1);
        }
        // Remove leading zeros: "0402" -> "402"
        while (s.length() > 1 && s.startsWith("0")) {
            s = s.substring(1);
        }
        return s;
    }

    private long parseFlatNumeric(String flatNumber) {
        try {
            return Long.parseLong(flatNumber.replaceAll("[^0-9]", ""));
        } catch (Exception e) {
            return 101L;
        }
    }
}
