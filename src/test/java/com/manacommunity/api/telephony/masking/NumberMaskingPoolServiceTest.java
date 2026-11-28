package com.manacommunity.api.telephony.masking;

import com.manacommunity.api.telephony.masking.model.MaskedCallSessionDto;
import com.manacommunity.api.telephony.masking.model.RouteInboundCallResultDto;
import com.manacommunity.api.telephony.masking.service.NumberMaskingPoolServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

class NumberMaskingPoolServiceTest {

    private NumberMaskingPoolServiceImpl service;
    private final Long communityId = 6002L;

    @BeforeEach
    void setUp() {
        service = new NumberMaskingPoolServiceImpl();
        service.initializePool(Arrays.asList("+918047120001", "+918047120002"));
    }

    @Test
    @DisplayName("Should allocate masked DID and route bidirectional calls without leaking personal numbers")
    void testMaskedCallAllocationAndRouting() {
        String riderPhone = "+919876500001";
        String residentPhone = "+919123450002";

        MaskedCallSessionDto session = service.allocateMaskedSession(communityId, riderPhone, residentPhone, 8);

        assertNotNull(session);
        assertEquals("+918047120001", session.getVirtualDid());
        assertEquals(MaskedCallSessionDto.State.ALLOCATED, session.getState());
        assertEquals(1, service.getAvailableDidCount());

        // Rider calls the virtual DID -> Routed to resident
        RouteInboundCallResultDto route1 = service.routeInboundCall(riderPhone, session.getVirtualDid());
        assertTrue(route1.isSuccess());
        assertEquals(residentPhone, route1.getRouteDestinationPhone());
        assertEquals(RouteInboundCallResultDto.RoutingStatus.ROUTED_ACTIVE_SESSION, route1.getStatus());

        // Resident calls the virtual DID back -> Routed to rider
        RouteInboundCallResultDto route2 = service.routeInboundCall(residentPhone, session.getVirtualDid());
        assertTrue(route2.isSuccess());
        assertEquals(riderPhone, route2.getRouteDestinationPhone());
    }

    @Test
    @DisplayName("Should route late callbacks during cooling-down quarantine window safely")
    void testQuarantineWindowLateCallback() {
        String riderPhone = "+919876500003";
        String residentPhone = "+919123450004";

        MaskedCallSessionDto session = service.allocateMaskedSession(communityId, riderPhone, residentPhone, 10);
        service.recordCallEnd(session.getSessionId());

        assertEquals(MaskedCallSessionDto.State.COOLING_DOWN, session.getState());

        // Rider calls back 2 minutes later ("I am at tower lobby")
        RouteInboundCallResultDto lateCallback = service.routeInboundCall(riderPhone, session.getVirtualDid());

        assertTrue(lateCallback.isSuccess());
        assertEquals(residentPhone, lateCallback.getRouteDestinationPhone());
        assertEquals(RouteInboundCallResultDto.RoutingStatus.ROUTED_QUARANTINE_CALLBACK, lateCallback.getStatus());
        assertEquals(1, session.getLateCallbackCount());
    }

    @Test
    @DisplayName("Should reject calls and safely reclaim DID when quarantine expires")
    void testReclaimExpiredSessions() {
        String riderPhone = "+919876500005";
        String residentPhone = "+919123450006";

        MaskedCallSessionDto session = service.allocateMaskedSession(communityId, riderPhone, residentPhone, 5);
        service.recordCallEnd(session.getSessionId());

        // Simulate quarantine expiration
        session.setCoolingExpiryTime(LocalDateTime.now().minusMinutes(1));

        // Call received after expiry -> automated IVR announcement
        RouteInboundCallResultDto result = service.routeInboundCall(riderPhone, session.getVirtualDid());
        assertFalse(result.isSuccess());
        assertEquals(RouteInboundCallResultDto.RoutingStatus.REJECTED_EXPIRED_IVR, result.getStatus());
        assertTrue(result.getIvrMessage().contains("session has expired"));

        // Reclaim expired sessions
        int reclaimed = service.reclaimExpiredSessions();
        assertEquals(1, reclaimed);
        assertEquals(2, service.getAvailableDidCount()); // DID returned to available pool
    }

    @Test
    @DisplayName("Should throw exception when DID pool is completely exhausted")
    void testDidPoolExhaustion() {
        service.allocateMaskedSession(communityId, "+919111111111", "+919222222222", 5);
        service.allocateMaskedSession(communityId, "+919333333333", "+919444444444", 5);

        // Pool has 2 DIDs, 3rd allocation must fail
        assertThrows(IllegalStateException.class, () ->
                service.allocateMaskedSession(communityId, "+919555555555", "+919666666666", 5));
    }
}
