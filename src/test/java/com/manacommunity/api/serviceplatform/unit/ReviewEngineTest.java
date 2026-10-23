package com.manacommunity.api.serviceplatform.unit;

import com.manacommunity.api.serviceplatform.review.engine.ReviewEngine;
import com.manacommunity.api.serviceplatform.review.entity.ServiceReview;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Review Engine Unit Tests")
class ReviewEngineTest {

    private ReviewEngine engine;

    @BeforeEach
    void setUp() {
        engine = new ReviewEngine();
    }

    @Test
    @DisplayName("Should compute average rating correctly")
    void testCalculateAverageRating() {
        List<ServiceReview> reviews = List.of(
                ServiceReview.builder().rating(5).build(),
                ServiceReview.builder().rating(4).build(),
                ServiceReview.builder().rating(5).build()
        );

        double avg = engine.calculateWeightedRating(reviews);
        assertEquals(4.67, avg);
    }

    @Test
    @DisplayName("Should return 0.0 for empty review list")
    void testEmptyReviews() {
        assertEquals(0.0, engine.calculateWeightedRating(Collections.emptyList()));
        assertEquals(0.0, engine.calculateWeightedRating(null));
    }

    @Test
    @DisplayName("Should validate rating bounds")
    void testValidateRatingRange() {
        assertTrue(engine.validateRatingRange(1));
        assertTrue(engine.validateRatingRange(5));
        assertFalse(engine.validateRatingRange(0));
        assertFalse(engine.validateRatingRange(6));
    }
}
