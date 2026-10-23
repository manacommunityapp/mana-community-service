package com.manacommunity.api.serviceplatform.unit;
import com.manacommunity.api.sports.model.*;
import com.manacommunity.api.sports.repository.*;
import com.manacommunity.api.sports.dto.*;
import com.manacommunity.api.sports.service.*;
import com.manacommunity.api.sports.scheduler.*;
import com.manacommunity.api.sports.controller.*;

import com.manacommunity.api.serviceplatform.entity.ServiceProvider;
import com.manacommunity.api.serviceplatform.recommendation.dto.ProviderMatchScoreDto;
import com.manacommunity.api.serviceplatform.recommendation.engine.RecommendationEngine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Recommendation Engine Unit Tests")
class RecommendationEngineTest {

    private RecommendationEngine engine;

    @BeforeEach
    void setUp() {
        engine = new RecommendationEngine();
    }

    @Test
    @DisplayName("Should score high-rated provider with more jobs higher")
    void testScoreProvider() {
        ServiceProvider topProvider = ServiceProvider.builder()
                .id(1L)
                .businessName("Apex Repairs")
                .avgRating(new BigDecimal("4.80"))
                .totalJobsCompleted(100)
                .build();

        ServiceProvider newProvider = ServiceProvider.builder()
                .id(2L)
                .businessName("Newbie Fix")
                .avgRating(new BigDecimal("3.50"))
                .totalJobsCompleted(2)
                .build();

        ProviderMatchScoreDto scoreTop = engine.scoreProvider(topProvider, 2.0);
        ProviderMatchScoreDto scoreNew = engine.scoreProvider(newProvider, 2.0);

        assertTrue(scoreTop.getOverallScore() > scoreNew.getOverallScore());
    }

    @Test
    @DisplayName("Should rank provider list descending by overall score")
    void testRankProviders() {
        ServiceProvider p1 = ServiceProvider.builder().id(1L).businessName("P1").avgRating(new BigDecimal("4.00")).totalJobsCompleted(10).build();
        ServiceProvider p2 = ServiceProvider.builder().id(2L).businessName("P2").avgRating(new BigDecimal("4.90")).totalJobsCompleted(50).build();

        List<ProviderMatchScoreDto> ranked = engine.rankProviders(List.of(p1, p2));

        assertEquals(2, ranked.size());
        assertEquals("P2", ranked.get(0).getProviderName());
        assertEquals("P1", ranked.get(1).getProviderName());
    }
}
