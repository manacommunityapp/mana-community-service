package com.manacommunity.api.fitprofile;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * Port for querying products eligible for jewellery sizing recommendations.
 */
public interface ProductCatalogPort {

    record Candidate(Long productId, String name, BigDecimal price) {}

    record ProductRef(Long productId, String category, String imageUrl) {}

    List<Candidate> availableByCategory(String category, int max);

    Optional<ProductRef> findAvailable(Long productId);
}
