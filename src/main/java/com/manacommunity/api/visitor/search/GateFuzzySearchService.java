package com.manacommunity.api.visitor.search;

import com.manacommunity.api.visitor.entity.WhitelistEntry;
import com.manacommunity.api.visitor.repository.WhitelistRepository;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * Low-latency (&lt;50ms) fuzzy typeahead lookup for security guards searching recurring
 * daily help (maids, drivers, cooks) or whitelisted vehicles by:
 * <ul>
 *   <li>Last 4 digits of license plates (normalized + reversed suffix matching)</li>
 *   <li>Partial phone digits (masked in output for PII safety)</li>
 *   <li>Prefix or misspelled names (bounded Levenshtein distance)</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
public class GateFuzzySearchService {

    private final WhitelistRepository whitelistRepository;

    @Transactional(readOnly = true)
    public List<GateSearchHit> searchTypeahead(Long communityId, String rawQuery, int limit) {
        if (communityId == null || rawQuery == null || rawQuery.isBlank()) {
            return List.of();
        }
        List<WhitelistEntry> entries = whitelistRepository.findByCommunityIdAndIsActiveTrue(communityId);
        return rankMatches(entries, rawQuery, Math.max(1, Math.min(limit, 25)));
    }

    /**
     * Pure ranking implementation also reusable against cached community shards.
     */
    public List<GateSearchHit> rankMatches(List<WhitelistEntry> entries, String rawQuery, int limit) {
        if (entries == null || entries.isEmpty() || rawQuery == null || rawQuery.isBlank()) {
            return List.of();
        }

        String qLower = rawQuery.trim().toLowerCase(Locale.ROOT);
        String qAlnum = normalizeAlnum(rawQuery);
        String qDigits = normalizeDigits(rawQuery);
        String qReversedAlnum = new StringBuilder(qAlnum).reverse().toString();

        List<GateSearchHit> hits = new ArrayList<>();

        for (WhitelistEntry entry : entries) {
            if (Boolean.FALSE.equals(entry.getIsActive())) continue;

            int bestScore = 0;
            String matchedOn = null;

            // 1. Vehicle plate suffix / reversed-prefix match (e.g. "1234" -> "KA-01-MJ-1234")
            String plateAlnum = normalizeAlnum(entry.getVehicleNumber());
            if (!qAlnum.isEmpty() && !plateAlnum.isEmpty()) {
                String plateReversed = new StringBuilder(plateAlnum).reverse().toString();
                if (plateAlnum.equals(qAlnum)) {
                    bestScore = 100;
                    matchedOn = "PLATE_EXACT";
                } else if (plateReversed.startsWith(qReversedAlnum) || plateAlnum.endsWith(qAlnum)) {
                    bestScore = Math.max(bestScore, 95);
                    matchedOn = "PLATE_SUFFIX";
                } else if (plateAlnum.contains(qAlnum)) {
                    bestScore = Math.max(bestScore, 80);
                    matchedOn = "PLATE_PARTIAL";
                }
            }

            // 2. Partial phone digits match (min 3 digits)
            String phoneDigits = normalizeDigits(entry.getPhone());
            if (qDigits.length() >= 3 && !phoneDigits.isEmpty()) {
                if (phoneDigits.endsWith(qDigits)) {
                    int s = 92;
                    if (s > bestScore) { bestScore = s; matchedOn = "PHONE_SUFFIX"; }
                } else if (phoneDigits.contains(qDigits)) {
                    int s = 82;
                    if (s > bestScore) { bestScore = s; matchedOn = "PHONE_PARTIAL"; }
                }
            }

            // 3. Name prefix or fuzzy match (supports misspellings like Suneeta -> Sunita)
            String name = entry.getName() != null ? entry.getName() : "";
            String nameLower = name.trim().toLowerCase(Locale.ROOT);
            if (!nameLower.isEmpty() && !qLower.isEmpty()) {
                if (nameLower.startsWith(qLower)) {
                    int s = 96;
                    if (s > bestScore) { bestScore = s; matchedOn = "NAME_PREFIX"; }
                } else if (nameLower.contains(qLower)) {
                    int s = 84;
                    if (s > bestScore) { bestScore = s; matchedOn = "NAME_SUBSTRING"; }
                } else {
                    int fuzzyScore = scoreFuzzyTokens(nameLower, qLower);
                    if (fuzzyScore > bestScore) {
                        bestScore = fuzzyScore;
                        matchedOn = "NAME_FUZZY";
                    }
                }
            }

            if (bestScore > 0) {
                hits.add(GateSearchHit.builder()
                        .id(entry.getId())
                        .name(entry.getName())
                        .category(entry.getCategory())
                        .vehicleNumber(entry.getVehicleNumber())
                        .maskedPhone(maskPhone(entry.getPhone()))
                        .flatNumber(entry.getResident() != null ? entry.getResident().getFlatNo() : null)
                        .score(bestScore)
                        .matchedOn(matchedOn)
                        .build());
            }
        }

        hits.sort(Comparator.comparingInt(GateSearchHit::getScore).reversed()
                .thenComparing(h -> h.getName() != null ? h.getName() : ""));
        return hits.size() > limit ? hits.subList(0, limit) : hits;
    }

    /**
     * Canonical Elasticsearch index settings + multi-field analyzers supporting
     * reversed plate suffix lookups, digit n-grams, and edge_ngram name autocomplete.
     */
    public Map<String, Object> getElasticsearchMappingSpec() {
        return Map.of(
                "settings", Map.of(
                        "number_of_shards", 6,
                        "analysis", Map.of(
                                "char_filter", Map.of(
                                        "alnum_only", Map.of("type", "pattern_replace", "pattern", "[^A-Za-z0-9]", "replacement", ""),
                                        "digits_only", Map.of("type", "pattern_replace", "pattern", "[^0-9]", "replacement", "")
                                ),
                                "filter", Map.of(
                                        "edge_2_15", Map.of("type", "edge_ngram", "min_gram", 2, "max_gram", 15),
                                        "ngram_3_10", Map.of("type", "ngram", "min_gram", 3, "max_gram", 10)
                                ),
                                "analyzer", Map.of(
                                        "plate_suffix_analyzer", Map.of(
                                                "type", "custom",
                                                "char_filter", List.of("alnum_only"),
                                                "tokenizer", "keyword",
                                                "filter", List.of("lowercase", "reverse", "edge_2_15")
                                        ),
                                        "phone_ngram_analyzer", Map.of(
                                                "type", "custom",
                                                "char_filter", List.of("digits_only"),
                                                "tokenizer", "keyword",
                                                "filter", List.of("ngram_3_10")
                                        ),
                                        "name_autocomplete_analyzer", Map.of(
                                                "type", "custom",
                                                "tokenizer", "standard",
                                                "filter", List.of("lowercase", "edge_2_15")
                                        )
                                )
                        )
                )
        );
    }

    private int scoreFuzzyTokens(String fullName, String query) {
        String[] nameTokens = fullName.split("\\s+");
        String[] queryTokens = query.split("\\s+");
        String qToken = queryTokens[0];
        if (qToken.length() < 3) return 0;

        int maxAllowedEdits = qToken.length() >= 6 ? 2 : 1;
        int best = 0;

        for (String token : nameTokens) {
            if (token.isEmpty()) continue;
            if (token.startsWith(qToken)) {
                best = Math.max(best, 88);
                continue;
            }
            int dist = levenshtein(token, qToken);
            if (dist <= maxAllowedEdits) {
                best = Math.max(best, dist == 1 ? 76 : 66);
            }
        }
        return best;
    }

    static int levenshtein(String a, String b) {
        int[][] dp = new int[a.length() + 1][b.length() + 1];
        for (int i = 0; i <= a.length(); i++) dp[i][0] = i;
        for (int j = 0; j <= b.length(); j++) dp[0][j] = j;
        for (int i = 1; i <= a.length(); i++) {
            for (int j = 1; j <= b.length(); j++) {
                int cost = a.charAt(i - 1) == b.charAt(j - 1) ? 0 : 1;
                dp[i][j] = Math.min(
                        Math.min(dp[i - 1][j] + 1, dp[i][j - 1] + 1),
                        dp[i - 1][j - 1] + cost
                );
            }
        }
        return dp[a.length()][b.length()];
    }

    private static String normalizeAlnum(String input) {
        return input == null ? "" : input.replaceAll("[^A-Za-z0-9]", "").toUpperCase(Locale.ROOT);
    }

    private static String normalizeDigits(String input) {
        return input == null ? "" : input.replaceAll("[^0-9]", "");
    }

    private static String maskPhone(String phone) {
        if (phone == null || phone.isBlank()) return null;
        String d = normalizeDigits(phone);
        if (d.length() < 4) return "****";
        return d.substring(0, 2) + "****" + d.substring(d.length() - 2);
    }

    @Data
    @Builder
    public static class GateSearchHit {
        private Long id;
        private String name;
        private String category;
        private String vehicleNumber;
        private String maskedPhone;
        private String flatNumber;
        private int score;
        private String matchedOn;
    }
}
