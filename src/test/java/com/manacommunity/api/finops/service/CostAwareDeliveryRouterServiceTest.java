package com.manacommunity.api.finops.service;

import com.manacommunity.api.finops.dto.FinOpsDtos.*;
import com.manacommunity.api.finops.entity.DeliveryRoutingEventEntity;
import com.manacommunity.api.finops.entity.ResidentChannelProfileEntity;
import com.manacommunity.api.finops.repository.DeliveryRoutingEventRepository;
import com.manacommunity.api.finops.repository.ResidentChannelProfileRepository;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

class CostAwareDeliveryRouterServiceTest {

    private static final ObjectProvider<StringRedisTemplate> NO_REDIS = new ObjectProvider<>() {
        @Override public StringRedisTemplate getObject(Object... args) { return null; }
        @Override public StringRedisTemplate getIfAvailable() { return null; }
        @Override public StringRedisTemplate getIfUnique() { return null; }
        @Override public StringRedisTemplate getObject() { return null; }
        @Override public Iterator<StringRedisTemplate> iterator() { return Stream.<StringRedisTemplate>empty().iterator(); }
    };

    @Test
    void routesOnlineResidentsToFreePushAndCancelsEscalationOnAck() {
        ResidentChannelProfileRepository profileRepo = Mockito.mock(ResidentChannelProfileRepository.class);
        DeliveryRoutingEventRepository eventRepo = Mockito.mock(DeliveryRoutingEventRepository.class);
        Map<UUID, DeliveryRoutingEventEntity> eventStore = new ConcurrentHashMap<>();

        ResidentChannelProfileEntity healthy = new ResidentChannelProfileEntity();
        healthy.setResidentId("res_1");
        healthy.setSocietyId(1L);
        healthy.setFcmAckRate7d(0.92);
        healthy.setFcmP90Ms(2500);
        healthy.setWaAckRate7d(0.85);
        healthy.setWaOptIn(true);

        when(profileRepo.findById("res_1")).thenReturn(Optional.of(healthy));
        when(eventRepo.save(any())).thenAnswer(inv -> {
            DeliveryRoutingEventEntity e = inv.getArgument(0);
            eventStore.put(e.getAlertId(), e);
            return e;
        });
        when(eventRepo.findById(any())).thenAnswer(inv -> Optional.ofNullable(eventStore.get(inv.getArgument(0))));

        CostAwareDeliveryRouterService router = new CostAwareDeliveryRouterService(profileRepo, eventRepo, NO_REDIS);

        DeliveryRoutePlanDto plan = router.dispatchAlert(new DispatchAlertRequest(
                1L, "res_1", "SWIGGY", 15_000, true, false, true, false
        ));

        assertEquals(CostAwareDeliveryRouterService.CH_PUSH, plan.initialChannel());
        assertEquals(0, plan.currentCostPaise());
        assertEquals(40, plan.projectedSavingsPaise());

        // Resident taps Push in 2.4s -> RESOLVED; subsequent escalation attempt must NOT incur WhatsApp/IVR cost!
        DeliveryEventDto acked = router.ackAlert(plan.alertId(), new AckAlertRequest(CostAwareDeliveryRouterService.CH_PUSH, 2400));
        assertEquals("RESOLVED", acked.status());
        assertTrue(acked.slaMet());
        assertEquals(0, acked.totalCostPaise());
        assertEquals(40, acked.savingsPaise());

        DeliveryEventDto postAckEscalate = router.escalateNext(plan.alertId());
        assertEquals("RESOLVED", postAckEscalate.status());
        assertEquals(0, postAckEscalate.totalCostPaise());
    }

    @Test
    void skipsDeadPushEarlyForAggressiveDozeAndDirectsToWhatsAppOrIvr() {
        ResidentChannelProfileRepository profileRepo = Mockito.mock(ResidentChannelProfileRepository.class);
        DeliveryRoutingEventRepository eventRepo = Mockito.mock(DeliveryRoutingEventRepository.class);
        when(eventRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ResidentChannelProfileEntity dozeUser = new ResidentChannelProfileEntity();
        dozeUser.setResidentId("res_doze");
        dozeUser.setSocietyId(1L);
        dozeUser.setOemAggressiveDoze(true);
        dozeUser.setFcmAckRate7d(0.15);
        dozeUser.setWaAckRate7d(0.80);
        dozeUser.setWaOptIn(true);
        when(profileRepo.findById("res_doze")).thenReturn(Optional.of(dozeUser));

        CostAwareDeliveryRouterService router = new CostAwareDeliveryRouterService(profileRepo, eventRepo, NO_REDIS);

        // Deterministic check via buildEscalationPlan without random probe
        List<EscalationStepDto> waFirst = router.buildEscalationPlan(dozeUser, 15_000, false, true, true, false);
        assertEquals(CostAwareDeliveryRouterService.CH_WA, waFirst.get(0).channel());
        assertEquals(0, waFirst.get(0).triggerAtMs());
        assertEquals(CostAwareDeliveryRouterService.CH_IVR, waFirst.get(1).channel());

        // Non-WhatsApp / offline resident skips straight to IVR
        List<EscalationStepDto> ivrFirst = router.buildEscalationPlan(dozeUser, 15_000, false, true, false, false);
        assertEquals(1, ivrFirst.size());
        assertEquals(CostAwareDeliveryRouterService.CH_IVR, ivrFirst.get(0).channel());
    }
}
