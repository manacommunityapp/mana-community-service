package com.manacommunity.api.fitprofile;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class RecommendationService {

    public record Recommendation(Long productId, String name, BigDecimal price, double score, String reason) {}

    public record Rule(String attribute, BigDecimal min, BigDecimal max, double weight) {}

    private final JdbcTemplate jdbc;
    private final FitProfileService profiles;
    private final ObjectProvider<ProductCatalogPort> catalog;

    /**
     * Returns product recommendations for a user based on their consented face shape.
     * Empty Optional indicates no fit profile exists (so UI can omit the "For You" row).
     */
    public Optional<List<Recommendation>> forUser(Long userId, String category, int limit) {
        Optional<String> faceShape = profiles.getFaceShape(userId);
        if (faceShape.isEmpty()) {
            return Optional.empty();
        }

        ProductCatalogPort port = catalog.getIfAvailable();
        if (port == null) {
            log.warn("No ProductCatalogPort bean found; returning empty recommendations");
            return Optional.of(List.of());
        }

        List<ProductCatalogPort.Candidate> candidates = port.availableByCategory(category, 300);
        if (candidates.isEmpty()) {
            return Optional.of(List.of());
        }

        List<Rule> rules = jdbc.query(
                "SELECT attribute, min_value, max_value, weight FROM recommendation_rules "
                        + "WHERE active AND category = ? AND face_shape = ?",
                (rs, i) -> new Rule(rs.getString("attribute"), rs.getBigDecimal("min_value"),
                        rs.getBigDecimal("max_value"), rs.getDouble("weight")),
                category.toUpperCase(), faceShape.get().toLowerCase());

        Map<Long, Map<String, BigDecimal>> attrs = loadAttributes(candidates);
        return Optional.of(rank(candidates, rules, attrs, limit));
    }

    public static List<Recommendation> rank(List<ProductCatalogPort.Candidate> candidates, List<Rule> rules,
                                            Map<Long, Map<String, BigDecimal>> attrs, int limit) {
        return candidates.stream()
                .map(c -> score(c, rules, attrs.getOrDefault(c.productId(), Map.of())))
                .filter(r -> r.score() > 0)
                .sorted(Comparator.comparingDouble(Recommendation::score).reversed()
                        .thenComparing(Recommendation::productId))
                .limit(limit)
                .toList();
    }

    private Map<Long, Map<String, BigDecimal>> loadAttributes(List<ProductCatalogPort.Candidate> candidates) {
        Long[] ids = candidates.stream().map(ProductCatalogPort.Candidate::productId).toArray(Long[]::new);
        Map<Long, Map<String, BigDecimal>> out = new HashMap<>();
        if (ids.length == 0) return out;

        jdbc.query(con -> {
            var ps = con.prepareStatement(
                    "SELECT product_id, drop_length_mm, size_mm, necklace_length_in "
                            + "FROM product_fit_attributes WHERE product_id = ANY (?)");
            ps.setArray(1, con.createArrayOf("bigint", ids));
            return ps;
        }, rs -> {
            Map<String, BigDecimal> m = new HashMap<>();
            m.put("drop_length_mm", rs.getBigDecimal("drop_length_mm"));
            m.put("size_mm", rs.getBigDecimal("size_mm"));
            m.put("necklace_length_in", rs.getBigDecimal("necklace_length_in"));
            out.put(rs.getLong("product_id"), m);
        });
        return out;
    }

    private static Recommendation score(ProductCatalogPort.Candidate c, List<Rule> rules, Map<String, BigDecimal> attrs) {
        double score = 0;
        for (Rule r : rules) {
            BigDecimal v = attrs.get(r.attribute());
            if (v == null) {
                continue;
            }
            boolean inRange = (r.min() == null || v.compareTo(r.min()) >= 0)
                    && (r.max() == null || v.compareTo(r.max()) <= 0);
            if (inRange) {
                score += r.weight();
            }
        }
        return new Recommendation(c.productId(), c.name(), c.price(), score,
                score > 0 ? "Suits your proportions" : "");
    }
}
