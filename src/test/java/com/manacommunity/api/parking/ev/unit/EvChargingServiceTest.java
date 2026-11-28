package com.manacommunity.api.parking.ev.unit;

import com.manacommunity.api.cfbos.wallet.engine.WalletEngine;
import com.manacommunity.api.cfbos.wallet.entity.CfbosWallet;
import com.manacommunity.api.cfbos.wallet.entity.CfbosWalletTransaction;
import com.manacommunity.api.cfbos.wallet.enums.WalletTransactionType;
import com.manacommunity.api.model.Community;
import com.manacommunity.api.parking.entity.ParkingSlot;
import com.manacommunity.api.parking.ev.dto.*;
import com.manacommunity.api.parking.ev.engine.EvBillingEngine;
import com.manacommunity.api.parking.ev.entity.*;
import com.manacommunity.api.parking.ev.repository.*;
import com.manacommunity.api.parking.ev.service.EvChargingService;
import com.manacommunity.api.parking.repository.ResidentVehicleRepository;
import com.manacommunity.api.user.model.AppUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("EvChargingService Unit Tests")
class EvChargingServiceTest {

    @Mock private EvChargerRepository chargerRepository;
    @Mock private EvChargingRateRepository rateRepository;
    @Mock private EvChargingSessionRepository sessionRepository;
    @Mock private EvTelemetryHeartbeatRepository heartbeatRepository;
    @Mock private ResidentVehicleRepository vehicleRepository;
    @Mock private WalletEngine walletEngine;
    @Mock private EvBillingEngine billingEngine;

    @InjectMocks
    private EvChargingService service;

    private Community testCommunity;
    private AppUser testResident;
    private EvCharger testCharger;

    @BeforeEach
    void setUp() {
        testCommunity = Community.builder().id(10L).name("Greenwood Society").build();
        testResident = AppUser.builder().id(55L).fullName("Rajesh Kumar").community(testCommunity).build();

        ParkingSlot slot = ParkingSlot.builder().id(101L).slotNumber("A-101").build();
        testCharger = EvCharger.builder()
                .id(1L)
                .deviceId("CHARGER-01")
                .community(testCommunity)
                .parkingSlot(slot)
                .status(EvCharger.ChargerStatus.AVAILABLE)
                .latestMeterKwh(100.0)
                .build();
    }

    @Test
    @DisplayName("startSession: starts session and updates charger to CHARGING")
    void startSession_success() {
        when(chargerRepository.findById(1L)).thenReturn(Optional.of(testCharger));
        when(sessionRepository.findFirstByResidentIdAndStatus(55L, EvChargingSession.SessionStatus.ACTIVE))
                .thenReturn(Optional.empty());
        when(rateRepository.findFirstByCommunityIdAndActiveTrue(10L)).thenReturn(Optional.empty());

        CfbosWallet wallet = CfbosWallet.builder().balance(new BigDecimal("500.00")).build();
        when(walletEngine.getOrCreateWallet(55L)).thenReturn(wallet);

        when(sessionRepository.save(any())).thenAnswer(inv -> {
            EvChargingSession s = inv.getArgument(0);
            s.setId(500L);
            return s;
        });

        StartChargingRequest req = new StartChargingRequest(1L, null, 100.0);
        EvChargingSessionResponse response = service.startSession(testResident, req);

        assertThat(response.id()).isEqualTo(500L);
        assertThat(response.status()).isEqualTo("ACTIVE");
        assertThat(testCharger.getStatus()).isEqualTo(EvCharger.ChargerStatus.CHARGING);
        verify(chargerRepository).save(testCharger);
    }

    @Test
    @DisplayName("stopSession: completes session, computes bill and debits wallet")
    void stopSession_success() {
        EvChargingSession activeSession = EvChargingSession.builder()
                .id(500L)
                .community(testCommunity)
                .charger(testCharger)
                .resident(testResident)
                .startedAt(LocalDateTime.now().minusHours(2))
                .startMeterKwh(100.0)
                .status(EvChargingSession.SessionStatus.ACTIVE)
                .build();

        when(sessionRepository.findById(500L)).thenReturn(Optional.of(activeSession));
        when(billingEngine.calculateKwh(100.0, 120.0)).thenReturn(20.0);
        when(billingEngine.calculateCost(eq(20.0), any(), any(), any())).thenReturn(new BigDecimal("200.00"));

        CfbosWalletTransaction txn = CfbosWalletTransaction.builder().id(777L).amount(new BigDecimal("200.00")).build();
        when(walletEngine.debitWallet(eq(55L), eq(new BigDecimal("200.00")), eq(WalletTransactionType.EV_CHARGING), anyString(), anyLong(), anyString()))
                .thenReturn(txn);

        when(sessionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        StopChargingRequest req = new StopChargingRequest(120.0, "USER_STOP");
        EvChargingSessionResponse response = service.stopSession(testResident, 500L, req);

        assertThat(response.status()).isEqualTo("COMPLETED");
        assertThat(response.totalCost()).isEqualByComparingTo("200.00");
        assertThat(response.totalKwh()).isEqualTo(20.0);
        assertThat(testCharger.getStatus()).isEqualTo(EvCharger.ChargerStatus.AVAILABLE);
    }

    @Test
    @DisplayName("processTelemetry: records heartbeat and evaluates charger status")
    void processTelemetry_success() {
        when(chargerRepository.findByDeviceId("CHARGER-01")).thenReturn(Optional.of(testCharger));
        when(sessionRepository.findFirstByChargerIdAndStatus(1L, EvChargingSession.SessionStatus.ACTIVE))
                .thenReturn(Optional.empty());

        EvTelemetryRequest req = new EvTelemetryRequest(
                "CHARGER-01",
                125.0,
                new BigDecimal("7.20"),
                new BigDecimal("230.0"),
                new BigDecimal("31.3"),
                new BigDecimal("38.0"),
                new BigDecimal("75.0"),
                null,
                LocalDateTime.now()
        );

        service.processTelemetry(req);

        verify(heartbeatRepository).save(any(EvTelemetryHeartbeat.class));
        assertThat(testCharger.getLatestMeterKwh()).isEqualTo(125.0);
        assertThat(testCharger.getCurrentPowerKw()).isEqualByComparingTo("7.20");
        assertThat(testCharger.getStatus()).isEqualTo(EvCharger.ChargerStatus.AVAILABLE);
        verify(chargerRepository).save(testCharger);
    }

    @Test
    @DisplayName("handlePowerCutInterruption: settles partial kWh, sets charger to FAULT POWER_CUT, and debits wallet once")
    void handlePowerCutInterruption_settlesPartialEnergyAndMarksFault() {
        EvChargingSession activeSession = EvChargingSession.builder()
                .id(501L).community(testCommunity).charger(testCharger).resident(testResident)
                .startedAt(LocalDateTime.now().minusMinutes(45)).startMeterKwh(100.0)
                .status(EvChargingSession.SessionStatus.ACTIVE).build();

        when(sessionRepository.findById(501L)).thenReturn(Optional.of(activeSession));
        when(billingEngine.calculateKwh(100.0, 108.5)).thenReturn(8.5);
        when(billingEngine.calculateCost(eq(8.5), any(), any(), any())).thenReturn(new BigDecimal("85.00"));
        when(walletEngine.debitWallet(eq(55L), eq(new BigDecimal("85.00")), eq(WalletTransactionType.EV_CHARGING),
                eq("EV_POWER_CUT_SETTLEMENT"), eq(501L), anyString()))
                .thenReturn(CfbosWalletTransaction.builder().id(888L).amount(new BigDecimal("85.00")).build());
        when(sessionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        EvChargingSessionResponse res = service.handlePowerCutInterruption(501L, 108.5);

        assertThat(res.status()).isEqualTo("COMPLETED");
        assertThat(res.stopReason()).isEqualTo("POWER_CUT");
        assertThat(res.totalKwh()).isEqualTo(8.5);
        assertThat(res.totalCost()).isEqualByComparingTo("85.00");
        assertThat(testCharger.getStatus()).isEqualTo(EvCharger.ChargerStatus.FAULT);
        assertThat(testCharger.getLastFaultCode()).isEqualTo("POWER_CUT");
    }

    @Test
    @DisplayName("evaluateIdleOverstayPenalty: triggers automatic wallet penalty only at 100% SoC beyond grace period")
    void evaluateIdleOverstayPenalty_triggersAt100PercentSoc() {
        EvChargingSession session = EvChargingSession.builder()
                .id(502L).community(testCommunity).charger(testCharger).resident(testResident)
                .startedAt(LocalDateTime.now().minusHours(3)).startMeterKwh(50.0)
                .status(EvChargingSession.SessionStatus.ACTIVE).build();
        when(sessionRepository.findById(502L)).thenReturn(Optional.of(session));

        // 85% SoC -> no overstay penalty even if idleMinutes > 0
        var below100 = service.evaluateIdleOverstayPenalty(502L, new BigDecimal("85"), 30L, new BigDecimal("60.00"));
        assertThat(below100.penaltyTriggered()).isFalse();
        assertThat(below100.penaltyAmount()).isEqualByComparingTo("0.00");

        // 100% SoC + 45 min past grace @ ₹60/hr -> ₹45.00 penalty debited & charger marked OCCUPIED_IDLE
        when(walletEngine.debitWallet(eq(55L), eq(new BigDecimal("45.00")), eq(WalletTransactionType.EV_CHARGING),
                eq("EV_OVERSTAY_PENALTY"), eq(502L), anyString()))
                .thenReturn(CfbosWalletTransaction.builder().id(999L).amount(new BigDecimal("45.00")).build());

        var overstay = service.evaluateIdleOverstayPenalty(502L, new BigDecimal("100"), 45L, new BigDecimal("60.00"));
        assertThat(overstay.penaltyTriggered()).isTrue();
        assertThat(overstay.penaltyAmount()).isEqualByComparingTo("45.00");
        assertThat(overstay.walletTxnId()).isEqualTo(999L);
        assertThat(testCharger.getStatus()).isEqualTo(EvCharger.ChargerStatus.OCCUPIED_IDLE);
    }
}
