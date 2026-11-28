package com.manacommunity.api.adtech.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Instant;
import java.util.Iterator;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class AdPacingRedisServiceTest {

    private static final ObjectProvider<StringRedisTemplate> NO_REDIS = new ObjectProvider<>() {
        @Override public StringRedisTemplate getObject(Object... args) { return null; }
        @Override public StringRedisTemplate getIfAvailable() { return null; }
        @Override public StringRedisTemplate getIfUnique() { return null; }
        @Override public StringRedisTemplate getObject() { return null; }
        @Override public Iterator<StringRedisTemplate> iterator() { return Stream.<StringRedisTemplate>empty().iterator(); }
    };

    @Test
    void enforcesDailyFrequencyCapAndImpressionDedup() {
        AdPacingRedisService pacing = new AdPacingRedisService(NO_REDIS);

        assertEquals(2, pacing.tryAcquireFrequencyCap("camp-1", "user-1", 3));
        assertEquals(1, pacing.tryAcquireFrequencyCap("camp-1", "user-1", 3));
        assertEquals(0, pacing.tryAcquireFrequencyCap("camp-1", "user-1", 3));
        assertEquals(-1, pacing.tryAcquireFrequencyCap("camp-1", "user-1", 3));

        assertTrue(pacing.markImpressionIfNew("imp-uuid-1"));
        assertFalse(pacing.markImpressionIfNew("imp-uuid-1"));
    }

    @Test
    void respectsCampaignHaltAndTokenBucket() {
        AdPacingRedisService pacing = new AdPacingRedisService(NO_REDIS);
        Instant start = Instant.now().minusSeconds(60);
        Instant end = Instant.now().plusSeconds(86400);

        assertTrue(pacing.tryConsumeBudgetToken("camp-2", 100_000_000L, 5000L, start, end));
        pacing.haltCampaign("camp-2");
        assertFalse(pacing.tryConsumeBudgetToken("camp-2", 100_000_000L, 5000L, start, end));
        pacing.unhaltCampaign("camp-2");
        assertTrue(pacing.tryConsumeBudgetToken("camp-2", 100_000_000L, 5000L, start, end));
    }
}
