package com.manacommunity.api.address.normalization.engine;

import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Fuzzy string matching engine combining Jaro-Winkler and Character Trigram cosine similarity.
 */
@Component
public class FuzzyStringMatcher {

    /**
     * Compute hybrid similarity score between 0.0 and 1.0.
     * Jaro-Winkler gives high accuracy for typographical prefix errors (Sarowar vs Sarovar).
     * Trigram similarity handles word-order permutations (Grande Aparna Sarovar).
     */
    public double computeSimilarity(String s1, String s2) {
        if (s1 == null || s2 == null) return 0.0;
        String a = s1.trim().toLowerCase();
        String b = s2.trim().toLowerCase();

        if (a.equals(b)) return 1.0;
        if (a.isEmpty() || b.isEmpty()) return 0.0;

        double jw = computeJaroWinkler(a, b);
        double trigram = computeTrigramSimilarity(a, b);

        return (0.50 * jw) + (0.50 * trigram);
    }

    /**
     * Jaro-Winkler distance calculation with standard Winkler prefix scaling (p = 0.1).
     */
    public double computeJaroWinkler(String s1, String s2) {
        int len1 = s1.length();
        int len2 = s2.length();
        int matchDistance = Math.max(len1, len2) / 2 - 1;

        boolean[] s1Matches = new boolean[len1];
        boolean[] s2Matches = new boolean[len2];

        int matches = 0;
        for (int i = 0; i < len1; i++) {
            int start = Math.max(0, i - matchDistance);
            int end = Math.min(i + matchDistance + 1, len2);

            for (int j = start; j < end; j++) {
                if (s2Matches[j]) continue;
                if (s1.charAt(i) != s2.charAt(j)) continue;
                s1Matches[i] = true;
                s2Matches[j] = true;
                matches++;
                break;
            }
        }

        if (matches == 0) return 0.0;

        // Count transpositions
        int k = 0;
        int transpositions = 0;
        for (int i = 0; i < len1; i++) {
            if (!s1Matches[i]) continue;
            while (!s2Matches[k]) k++;
            if (s1.charAt(i) != s2.charAt(k)) {
                transpositions++;
            }
            k++;
        }

        double jaro = ((matches / (double) len1)
                + (matches / (double) len2)
                + ((matches - transpositions / 2.0) / matches)) / 3.0;

        // Winkler prefix bonus (up to 4 matching initial chars)
        int prefix = 0;
        for (int i = 0; i < Math.min(4, Math.min(len1, len2)); i++) {
            if (s1.charAt(i) == s2.charAt(i)) prefix++;
            else break;
        }

        return jaro + (prefix * 0.1 * (1.0 - jaro));
    }

    /**
     * Character Trigram similarity (Sørensen–Dice coefficient over 3-grams).
     */
    public double computeTrigramSimilarity(String s1, String s2) {
        Set<String> set1 = buildTrigrams("  " + s1 + " ");
        Set<String> set2 = buildTrigrams("  " + s2 + " ");

        if (set1.isEmpty() && set2.isEmpty()) return 1.0;
        if (set1.isEmpty() || set2.isEmpty()) return 0.0;

        int intersection = 0;
        for (String tri : set1) {
            if (set2.contains(tri)) {
                intersection++;
            }
        }

        return (2.0 * intersection) / (set1.size() + set2.size());
    }

    private Set<String> buildTrigrams(String s) {
        Set<String> set = new HashSet<>();
        for (int i = 0; i <= s.length() - 3; i++) {
            set.add(s.substring(i, i + 3));
        }
        return set;
    }
}
