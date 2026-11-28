package com.manacommunity.api.platform.tenant;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TenantConnectionQuotaManagerTest {

    private TenantConnectionQuotaManager quotaManager;

    @BeforeEach
    void setUp() {
        // Default quota of 3 connections per tenant with 10ms acquire timeout for fast testing
        quotaManager = new TenantConnectionQuotaManager(3, 10);
    }

    @Test
    void testNormalAcquireAndAutoRelease() {
        Long communityId = 101L;

        assertEquals(3, quotaManager.getAvailableLeases(communityId));
        assertEquals(0, quotaManager.getActiveLeases(communityId));

        try (TenantConnectionQuotaManager.TenantLease lease1 = quotaManager.acquireLease(communityId)) {
            assertEquals(1, quotaManager.getActiveLeases(communityId));
            assertEquals(2, quotaManager.getAvailableLeases(communityId));
        }

        // Released upon close
        assertEquals(0, quotaManager.getActiveLeases(communityId));
        assertEquals(3, quotaManager.getAvailableLeases(communityId));
    }

    @Test
    void testNoisyTenantExhaustionThrowsExceptionWithoutStarvingOtherTenants() {
        Long megaTownshipId = 101L;
        Long smallApartmentId = 102L;

        // Mega-township acquires all 3 of its allowed leases
        TenantConnectionQuotaManager.TenantLease lease1 = quotaManager.acquireLease(megaTownshipId);
        TenantConnectionQuotaManager.TenantLease lease2 = quotaManager.acquireLease(megaTownshipId);
        TenantConnectionQuotaManager.TenantLease lease3 = quotaManager.acquireLease(megaTownshipId);

        assertEquals(3, quotaManager.getActiveLeases(megaTownshipId));
        assertEquals(0, quotaManager.getAvailableLeases(megaTownshipId));

        // Mega-township 4th lease attempt is blocked and fails fast
        TenantConnectionQuotaExceededException ex = assertThrows(
                TenantConnectionQuotaExceededException.class,
                () -> quotaManager.acquireLease(megaTownshipId)
        );
        assertEquals(megaTownshipId, ex.getCommunityId());
        assertEquals(3, ex.getActiveLeases());
        assertEquals(3, ex.getMaxAllowed());

        // CRITICAL ISOLATION TEST: Small apartment (community 102) can still acquire leases freely!
        try (TenantConnectionQuotaManager.TenantLease smallLease = quotaManager.acquireLease(smallApartmentId)) {
            assertEquals(1, quotaManager.getActiveLeases(smallApartmentId));
            assertEquals(2, quotaManager.getAvailableLeases(smallApartmentId));
        }

        // Cleanup
        lease1.close();
        lease2.close();
        lease3.close();

        assertEquals(0, quotaManager.getActiveLeases(megaTownshipId));
    }

    @Test
    void testCustomTenantQuotaOverride() {
        Long vipTownshipId = 999L;
        quotaManager.setTenantQuota(vipTownshipId, 5);

        assertEquals(5, quotaManager.getAvailableLeases(vipTownshipId));

        TenantConnectionQuotaManager.TenantLease l1 = quotaManager.acquireLease(vipTownshipId);
        TenantConnectionQuotaManager.TenantLease l2 = quotaManager.acquireLease(vipTownshipId);
        TenantConnectionQuotaManager.TenantLease l3 = quotaManager.acquireLease(vipTownshipId);
        TenantConnectionQuotaManager.TenantLease l4 = quotaManager.acquireLease(vipTownshipId);
        TenantConnectionQuotaManager.TenantLease l5 = quotaManager.acquireLease(vipTownshipId);

        assertEquals(5, quotaManager.getActiveLeases(vipTownshipId));

        assertThrows(TenantConnectionQuotaExceededException.class,
                () -> quotaManager.acquireLease(vipTownshipId));

        l1.close();
        l2.close();
        l3.close();
        l4.close();
        l5.close();
    }
}
