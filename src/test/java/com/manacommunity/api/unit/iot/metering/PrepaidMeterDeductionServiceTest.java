package com.manacommunity.api.unit.iot.metering;

import com.manacommunity.api.iot.edge.entity.PrepaidMeterAccount;
import com.manacommunity.api.iot.edge.repository.PrepaidMeterAccountRepository;
import com.manacommunity.api.iot.edge.service.PrepaidMeterDeductionService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Prepaid Smart Meter Out-of-Order Telemetry, DG/Grid Tariff Switching & Cutoff Tests")
class PrepaidMeterDeductionServiceTest {

    @Mock
    private PrepaidMeterAccountRepository accountRepository;

    @InjectMocks
    private PrepaidMeterDeductionService deductionService;

    @Test
    @DisplayName("Rejects out-of-order 15m telemetry, switches between Grid & DG tariffs, and disconnects at <= 0 balance")
    void shouldRejectOutOfOrderSwitchDgTariffAndDisconnectOnZeroBalance() {
        PrepaidMeterAccount account = PrepaidMeterAccount.builder()
                .id(1L)
                .meterId(401L)
                .communityId(10L)
                .unitNumber("B-504")
                .balance(new BigDecimal("200.0000"))
                .lowBalanceThreshold(new BigDecimal("100.00"))
                .ratePerUnit(new BigDecimal("8.0000"))
                .dgRatePerUnit(new BigDecimal("22.0000"))
                .activeTariffSource("GRID")
                .lastSequenceNumber(10L)
                .active(true)
                .build();

        when(accountRepository.findByMeterIdForUpdate(401L)).thenReturn(Optional.of(account));
        when(accountRepository.findByMeterId(401L)).thenReturn(Optional.of(account));
        when(accountRepository.save(any(PrepaidMeterAccount.class))).thenAnswer(inv -> inv.getArgument(0));

        // 1. Out-of-order / duplicate 15m packet (seq 9 <= last 10) rejected without balance deduction
        var stale = deductionService.processTelemetryReading(401L, 9L, new BigDecimal("5.0"), Instant.now());
        assertThat(stale.success()).isFalse();
        assertThat(stale.message()).contains("Duplicate/out-of-order");
        assertThat(account.getBalance()).isEqualByComparingTo("200.0000");

        // 2. Grid interval consumption: 10 kWh * ₹8.00 = ₹80.00 -> balance ₹120.00
        var gridRes = deductionService.processTelemetryReading(401L, 11L, new BigDecimal("10.0"), Instant.now());
        assertThat(gridRes.success()).isTrue();
        assertThat(gridRes.amountDeducted()).isEqualByComparingTo("80.0000");
        assertThat(gridRes.remainingBalance()).isEqualByComparingTo("120.0000");
        assertThat(gridRes.lowBalanceAlert()).isFalse();

        // 3. Power outage -> switch to DG_BACKUP (₹22.00/kWh): 2 kWh -> ₹44.00 -> balance ₹76.00 (<100 threshold)
        deductionService.switchTariffSource(401L, "DG_BACKUP");
        var dgLowAlert = deductionService.processTelemetryReading(401L, 12L, new BigDecimal("2.0"), Instant.now());
        assertThat(dgLowAlert.amountDeducted()).isEqualByComparingTo("44.0000");
        assertThat(dgLowAlert.remainingBalance()).isEqualByComparingTo("76.0000");
        assertThat(dgLowAlert.lowBalanceAlert()).isTrue();
        assertThat(dgLowAlert.disconnected()).isFalse();

        // 4. Exhaust remaining balance on DG -> 4 kWh * ₹22.00 = ₹88.00 -> balance -₹12.00 -> auto disconnect
        var cutoff = deductionService.processTelemetryReading(401L, 13L, new BigDecimal("4.0"), Instant.now());
        assertThat(cutoff.remainingBalance()).isEqualByComparingTo("-12.0000");
        assertThat(cutoff.disconnected()).isTrue();
        assertThat(account.getIsDisconnected()).isTrue();

        // 5. Recharge ₹500 restores supply and clears low balance alert
        deductionService.rechargeAccount(401L, new BigDecimal("500.00"));
        assertThat(account.getBalance()).isEqualByComparingTo("488.0000");
        assertThat(account.getIsDisconnected()).isFalse();
        assertThat(account.getLowBalanceAlertSent()).isFalse();
    }
}
