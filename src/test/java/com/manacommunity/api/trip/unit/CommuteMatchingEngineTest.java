package com.manacommunity.api.trip.unit;

import com.manacommunity.api.trip.engine.CommuteMatchingEngine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Commute Matching & Proximity Engine Unit Tests")
class CommuteMatchingEngineTest {

    private CommuteMatchingEngine matchingEngine;

    @BeforeEach
    void setUp() {
        matchingEngine = new CommuteMatchingEngine();
    }

    @Test
    @DisplayName("Should calculate Haversine distance correctly between two coordinates")
    void shouldCalculateHaversineDistance() {
        // Bangalore Central (12.9716, 77.5946) to Whitefield (12.9698, 77.7500) ~ 16.8 km
        double dist = matchingEngine.calculateDistanceKm(12.9716, 77.5946, 12.9698, 77.7500);
        assertTrue(dist > 15.0 && dist < 19.0, "Distance should be approx 16.8km but was " + dist);
    }

    @Test
    @DisplayName("Should detect compatible route within detour threshold")
    void shouldDetectCompatibleRoute() {
        // Driver from (12.9716, 77.5946) to (12.9698, 77.7500)
        // Rider from nearby pickup (12.9720, 77.5950) to nearby dropoff (12.9700, 77.7490)
        boolean compatible = matchingEngine.isRouteCompatible(
                12.9716, 77.5946, 12.9698, 77.7500,
                12.9720, 77.5950, 12.9700, 77.7490,
                2.0
        );
        assertTrue(compatible);
    }

    @Test
    @DisplayName("Should enforce ladies-only booking restriction")
    void shouldEnforceLadiesOnlyRestriction() {
        assertTrue(matchingEngine.canRiderBook(false, "MALE"));
        assertTrue(matchingEngine.canRiderBook(true, "FEMALE"));
        assertFalse(matchingEngine.canRiderBook(true, "MALE"));
    }
}
