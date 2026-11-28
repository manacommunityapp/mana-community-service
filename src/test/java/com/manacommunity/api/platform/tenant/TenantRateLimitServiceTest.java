package com.manacommunity.api.platform.tenant;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TenantRateLimitServiceTest {

    private TenantRateLimitService rateLimitService;

    @BeforeEach
    void setUp() {
        // Instantiate without Redis to exercise the thread-safe sliding window in-memory fallback
        // Default limit of 5 requests per minute for fast and deterministic unit testing
        rateLimitService = new TenantRateLimitService(null, 5);
    }

    @Test
    void testNormalRequestsAllowedWithinQuota() {
        Long communityId = 101L;

        for (int i = 1; i <= 5; i++) {
            TenantRateLimitService.TenantRateLimitResult result = rateLimitService.checkTenant(communityId);
            assertTrue(result.allowed(), "Request " + i + " should be allowed");
            assertEquals(5, result.limit());
            assertEquals(5 - i, result.remaining());
            assertTrue(result.resetEpochSecond() > 0);
        }
    }

    @Test
    void testExceedingQuotaBlocksNoisyTenantWithoutImpactingOtherTenants() {
        Long noisyTownshipId = 101L;
        Long quietApartmentId = 102L;

        // Exhaust quota for noisy township (5 requests)
        for (int i = 0; i < 5; i++) {
            assertTrue(rateLimitService.checkTenant(noisyTownshipId).allowed());
        }

        // 6th request for noisy township must be blocked
        TenantRateLimitService.TenantRateLimitResult blocked = rateLimitService.checkTenant(noisyTownshipId);
        assertFalse(blocked.allowed(), "6th request from noisy township should be throttled");
        assertEquals(0, blocked.remaining());
        assertEquals(5, blocked.limit());

        // 7th request also blocked
        assertFalse(rateLimitService.checkTenant(noisyTownshipId).allowed());

        // CRITICAL ISOLATION: Quiet apartment (community 102) is completely unaffected!
        for (int i = 1; i <= 5; i++) {
            TenantRateLimitService.TenantRateLimitResult quietResult = rateLimitService.checkTenant(quietApartmentId);
            assertTrue(quietResult.allowed(), "Quiet apartment request " + i + " must succeed");
            assertEquals(5 - i, quietResult.remaining());
        }
    }

    @Test
    void testCustomTenantLimitOverride() {
        Long megaTownshipId = 999L;
        rateLimitService.setTenantLimit(megaTownshipId, 10);

        for (int i = 1; i <= 10; i++) {
            TenantRateLimitService.TenantRateLimitResult res = rateLimitService.checkTenant(megaTownshipId);
            assertTrue(res.allowed());
            assertEquals(10, res.limit());
            assertEquals(10 - i, res.remaining());
        }

        // 11th request is blocked
        TenantRateLimitService.TenantRateLimitResult blocked = rateLimitService.checkTenant(megaTownshipId);
        assertFalse(blocked.allowed());
        assertEquals(0, blocked.remaining());
    }

    @Test
    void testPlatformOrNullCommunityPassesThrough() {
        TenantRateLimitService.TenantRateLimitResult nullResult = rateLimitService.checkTenant(null);
        assertTrue(nullResult.allowed());
        assertEquals(10000, nullResult.limit());

        TenantRateLimitService.TenantRateLimitResult zeroResult = rateLimitService.checkTenant(0L);
        assertTrue(zeroResult.allowed());

        TenantRateLimitService.TenantRateLimitResult negativeResult = rateLimitService.checkTenant(-1L);
        assertTrue(negativeResult.allowed());
    }

    @Test
    void testResetClearsState() {
        Long communityId = 300L;
        rateLimitService.setTenantLimit(communityId, 2);

        rateLimitService.checkTenant(communityId);
        rateLimitService.checkTenant(communityId);
        assertFalse(rateLimitService.checkTenant(communityId).allowed());

        rateLimitService.reset();

        // After reset, reverts to default quota of 5 and empty counters
        TenantRateLimitService.TenantRateLimitResult freshResult = rateLimitService.checkTenant(communityId);
        assertTrue(freshResult.allowed());
        assertEquals(5, freshResult.limit());
        assertEquals(4, freshResult.remaining());
    }
}
