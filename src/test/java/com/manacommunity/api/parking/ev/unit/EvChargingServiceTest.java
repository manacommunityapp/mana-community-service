package com.manacommunity.api.parking.ev.unit;

import com.manacommunity.api.cfbos.wallet.engine.WalletEngine;
import com.manacommunity.api.cfbos.wallet.entity.CfbosWallet;
import com.manacommunity.api.cfbos.wallet.entity.CfbosWalletTransaction;
import com.manacommunity.api.cfbos.wallet.enums.WalletTransactionType;
import com.manacommunity.api.parking.ev.dto.*;
import com.manacommunity.api.parking.ev.engine.EvChargingBillingEngine;
import com.manacommunity.api.parking.ev.engine.EvTelemetryEngine;
import com.manacommunity.api.parking.ev.entity.*;
import com.manacommunity.api.parking.ev.enums.*;
import com.manacommunity.api.parking.ev.repository.*;
import com.manacommunity.api.parking.ev.service.impl.EvChargingServiceImpl;
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

    @Mock private EvChargingStationRepository stationRepository;
    @Mock private EvChargingSessionRepository sessionRepository;
    @Mock private EvTelemetryHeartbeatRepository heartbeatRepository;
    @Mock private EvTariffConfigRepository tariffRepository;
    @Mock private EvTelemetryEngine telemetryEngine;
    @Mock private EvChargingBillingEngine billingEngine;
    @Mock private WalletEngine walletEngine;

    @InjectMocks
    private EvChargingServiceImpl service;

    private EvChargingStation testStation;

    @BeforeEach
    void setUp() {
        testStation = EvChargingStation.builder()
                .id(1L)
                .hardwareId("STATION-01")
                .name("Tower A - Slot 101 EV")
                .communityId(10L)
                .parkingSlotId(101L)
                .status(EvStationStatus.AVAILABLE)
                .latestMeterKwh(new BigDecimal("100.000"))
                .build();
    }

    @Test
    @DisplayName("startSession: successfully starts session when station is available")
    void startSession_success() {
        when(stationRepository.findById(1L)).thenReturn(Optional.of(testStation));
        when(sessionRepository.save(any(EvChargingSession.class))).thenAnswer(inv -> {
            EvChargingSession s = inv.getArgument(0);
            s.setId(500L);
            return s;
        });

        EvStartSessionRequest req = new EvStartSessionRequest(1L, "KA01AB1234", new BigDecimal("100.000"));
        EvSessionResponse response = service.startSession(55L, 10L, req);

        assertThat(response.id()).isEqualTo(500L);
        assertThat(response.status()).isEqualTo(EvSessionStatus.IN_PROGRESS);
        assertThat(response.paymentStatus()).isEqualTo(EvPaymentStatus.PENDING);
        assertThat(testStation.getStatus()).isEqualTo(EvStationStatus.CHARGING);
        verify(stationRepository).save(testStation);
    }

    @Test
    @DisplayName("startSession: throws when station is already charging")
    void startSession_alreadyCharging_throws() {
        testStation.setStatus(EvStationStatus.CHARGING);
        when(stationRepository.findById(1L)).thenReturn(Optional.of(testStation));

        EvStartSessionRequest req = new EvStartSessionRequest(1L, "KA01AB1234", new BigDecimal("100.000"));
        assertThatThrownBy(() -> service.startSession(55L, 10L, req))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("occupied or charging");
    }

    @Test
    @DisplayName("stopSession: completes session and settles via CFBOS wallet")
    void stopSession_settlesViaWallet() {
        EvChargingSession activeSession = EvChargingSession.builder()
                .id(500L)
                .stationId(1L)
                .residentId(55L)
                .communityId(10L)
                .startTime(LocalDateTime.now().minusHours(2))
                .initialMeterKwh(new BigDecimal("100.000"))
                .status(EvSessionStatus.IN_PROGRESS)
                .paymentStatus(EvPaymentStatus.PENDING)
                .build();

        when(sessionRepository.findById(500L)).thenReturn(Optional.of(activeSession));
        when(stationRepository.findById(1L)).thenReturn(Optional.of(testStation));
        when(tariffRepository.findByCommunityIdAndActiveTrue(10L)).thenReturn(Optional.empty());

        EvChargingBillingEngine.BillBreakdown bill = new EvChargingBillingEngine.BillBreakdown(
                new BigDecimal("20.000"),
                new BigDecimal("200.00"),
                BigDecimal.ZERO,
                new BigDecimal("200.00")
        );
        when(billingEngine.calculateBill(any(), any(), any(), any(), any(), any())).thenReturn(bill);

        CfbosWallet mockWallet = CfbosWallet.builder()
                .id(1L)
                .residentId(55L)
                .balance(new BigDecimal("500.00"))
                .build();
        when(walletEngine.getOrCreateWallet(55L)).thenReturn(mockWallet);

        CfbosWalletTransaction mockTxn = CfbosWalletTransaction.builder()
                .id(999L)
                .amount(new BigDecimal("200.00"))
                .build();
        when(walletEngine.debitWallet(eq(55L), eq(new BigDecimal("200.00")), eq(WalletTransactionType.EV_CHARGING), anyString(), anyLong(), anyString()))
                .thenReturn(mockTxn);

        when(sessionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        EvStopSessionRequest req = new EvStopSessionRequest(new BigDecimal("120.000"), "MANUAL", 0);
        EvSessionResponse response = service.stopSession(500L, 55L, req);

        assertThat(response.status()).isEqualTo(EvSessionStatus.COMPLETED);
        assertThat(response.paymentStatus()).isEqualTo(EvPaymentStatus.SETTLED_WALLET);
        assertThat(response.walletTransactionId()).isEqualTo(999L);
        assertThat(response.totalCost()).isEqualByComparingTo("200.00");
        assertThat(testStation.getStatus()).isEqualTo(EvStationStatus.AVAILABLE);
    }

    @Test
    @DisplayName("stopSession: falls back to utility billing if wallet balance insufficient")
    void stopSession_insufficientWallet_billedUtility() {
        EvChargingSession activeSession = EvChargingSession.builder()
                .id(500L)
                .stationId(1L)
                .residentId(55L)
                .communityId(10L)
                .startTime(LocalDateTime.now().minusHours(2))
                .initialMeterKwh(new BigDecimal("100.000"))
                .status(EvSessionStatus.IN_PROGRESS)
                .paymentStatus(EvPaymentStatus.PENDING)
                .build();

        when(sessionRepository.findById(500L)).thenReturn(Optional.of(activeSession));
        when(stationRepository.findById(1L)).thenReturn(Optional.of(testStation));

        EvChargingBillingEngine.BillBreakdown bill = new EvChargingBillingEngine.BillBreakdown(
                new BigDecimal("20.000"),
                new BigDecimal("200.00"),
                BigDecimal.ZERO,
                new BigDecimal("200.00")
        );
        when(billingEngine.calculateBill(any(), any(), any(), any(), any(), any())).thenReturn(bill);

        CfbosWallet mockWallet = CfbosWallet.builder()
                .id(1L)
                .residentId(55L)
                .balance(new BigDecimal("50.00")) // insufficient
                .build();
        when(walletEngine.getOrCreateWallet(55L)).thenReturn(mockWallet);
        when(sessionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        EvStopSessionRequest req = new EvStopSessionRequest(new BigDecimal("120.000"), "MANUAL", 0);
        EvSessionResponse response = service.stopSession(500L, 55L, req);

        assertThat(response.status()).isEqualTo(EvSessionStatus.COMPLETED);
        assertThat(response.paymentStatus()).isEqualTo(EvPaymentStatus.BILLED_UTILITY);
        assertThat(response.walletTransactionId()).isNull();
    }
}
