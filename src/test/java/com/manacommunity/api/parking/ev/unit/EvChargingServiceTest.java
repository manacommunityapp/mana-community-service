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
}
