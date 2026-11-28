package com.manacommunity.api.parking.service;

import com.manacommunity.api.parking.entity.EvCharger;
import com.manacommunity.api.parking.entity.EvChargingSession;
import com.manacommunity.api.parking.repository.EvChargerRepository;
import com.manacommunity.api.parking.repository.EvChargingSessionRepository;
import com.manacommunity.api.parking.service.EvChargerQueueService.ReservationResult;
import com.manacommunity.api.parking.service.EvChargerQueueService.SessionEndResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EvChargerQueueServiceTest {

    @Mock private EvChargerRepository chargerRepo;
    @Mock private EvChargingSessionRepository sessionRepo;

    private EvChargerQueueService queueService;

    @BeforeEach
    void setUp() {
        queueService = new EvChargerQueueService(chargerRepo, sessionRepo);
    }

    @Test
    void promotesQueueAndAssessesOverstayPenaltyWhenExceedingMaxSessionWindow() {
        EvCharger charger = new EvCharger();
        charger.setId(1L);
        charger.setCommunityId(101L);
        charger.setChargerCode("OCPP-BAY-01");
        charger.setPowerKw(new BigDecimal("10.0"));
        charger.setRatePerKwh(new BigDecimal("12.00"));
        charger.setOverstayPenaltyPerHour(new BigDecimal("60.00"));
        charger.setMaxSessionMinutes(60);
        charger.setStatus("CHARGING");

        // Session started 90 min ago -> 60 min max -> 30 min overstay
        EvChargingSession active = new EvChargingSession();
        active.setId(55L);
        active.setCommunityId(101L);
        active.setChargerId(1L);
        active.setFlatNumber("A-402");
        active.setUserId(10L);
        active.setVehicleNumber("KA01EV1234");
        active.setStatus("CHARGING");
        active.setStartTime(Instant.now().minus(90, ChronoUnit.MINUTES));
        active.setMaxSessionMinutes(60);

        EvChargingSession waiting = new EvChargingSession();
        waiting.setId(56L);
        waiting.setCommunityId(101L);
        waiting.setChargerId(0L);
        waiting.setFlatNumber("B-101");
        waiting.setUserId(11L);
        waiting.setVehicleNumber("KA01EV5678");
        waiting.setStatus("QUEUED");
        waiting.setQueuePosition(1);

        List<EvChargingSession> queue = new ArrayList<>(List.of(waiting));

        when(sessionRepo.findById(55L)).thenReturn(Optional.of(active));
        when(chargerRepo.findById(1L)).thenReturn(Optional.of(charger));
        when(sessionRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(chargerRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(sessionRepo.findAllQueued(101L)).thenReturn(queue);

        SessionEndResult ended = queueService.endSession(55L);

        assertThat(ended.status()).isEqualTo("OVERSTAY");
        assertThat(ended.overstayMinutes()).isGreaterThanOrEqualTo(29);
        assertThat(ended.energyKwh()).isGreaterThanOrEqualTo(new BigDecimal("14.8"));
        assertThat(ended.overstayPenalty()).isGreaterThan(BigDecimal.ZERO);
        assertThat(ended.totalAmount()).isEqualByComparingTo(ended.energyCost().add(ended.overstayPenalty()));

        // Next queued vehicle promoted automatically and charger occupied again
        assertThat(waiting.getStatus()).isEqualTo("CHARGING");
        assertThat(waiting.getChargerId()).isEqualTo(1L);
        assertThat(charger.getStatus()).isEqualTo("CHARGING");
    }

    @Test
    void queuesRequestWhenAllOcppChargersBusy() {
        when(chargerRepo.findAvailableChargers(101L)).thenReturn(List.of());
        when(sessionRepo.findAllQueued(101L)).thenReturn(List.of());
        when(sessionRepo.save(any())).thenAnswer(inv -> {
            EvChargingSession s = inv.getArgument(0);
            s.setId(88L);
            return s;
        });

        ReservationResult queued = queueService.reserveCharger(101L, "C-204", 22L, "KA05EV9999", null);

        assertThat(queued.startedCharging()).isFalse();
        assertThat(queued.queuePosition()).isEqualTo(1);
        assertThat(queued.sessionId()).isEqualTo(88L);
    }
}
