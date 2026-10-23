package com.manacommunity.api.unit.iot.metering;

import com.manacommunity.api.iot.metering.*;
import com.manacommunity.api.iot.metering.MeteringEnums.*;
import com.manacommunity.api.iot.metering.dto.MeteringDtos.*;
import com.manacommunity.api.iot.metering.engine.MeterPulseIngestionEngine;
import com.manacommunity.api.iot.metering.engine.UtilityTierSlabEngine;
import com.manacommunity.api.model.Community;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("SmartMeterService Unit Tests")
public class SmartMeterServiceTest {

    @Mock
    private SmartMeterRepository meterRepository;

    @Mock
    private MeterTelemetryReadingRepository readingRepository;

    @Mock
    private MeterBillingCycleSummaryRepository summaryRepository;

    @Spy
    private MeterPulseIngestionEngine ingestionEngine = new MeterPulseIngestionEngine();

    @Spy
    private UtilityTierSlabEngine slabEngine = new UtilityTierSlabEngine();

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @InjectMocks
    private SmartMeterServiceImpl service;

    private SmartMeter mockMeter;
    private Community mockCommunity;

    @BeforeEach
    void setUp() {
        mockCommunity = Community.builder().id(1L).name("Mana Community").build();
        mockMeter = SmartMeter.builder()
                .id(10L)
                .meterSerialNumber("EM-T1-101")
                .meterType(MeterType.ELECTRICITY_METER)
                .community(mockCommunity)
                .unitNumber("A-101")
                .pulseMultiplier(BigDecimal.ONE)
                .lastReading(new BigDecimal("100.0"))
                .lastPulseCount(100L)
                .status(MeterStatus.ACTIVE)
                .build();
    }

    @Test
    @DisplayName("Ingest telemetry saves reading and broadcasts update")
    void testIngestTelemetry() {
        when(meterRepository.findByMeterSerialNumber("EM-T1-101")).thenReturn(Optional.of(mockMeter));

        IngestTelemetryRequest req = IngestTelemetryRequest.builder()
                .meterSerialNumber("EM-T1-101")
                .pulseCount(150L)
                .voltage(new BigDecimal("230.5"))
                .current(new BigDecimal("12.0"))
                .build();

        var result = service.ingestTelemetry(req);

        assertNotNull(result);
        assertEquals(new BigDecimal("50.0000"), result.getDeltaConsumed());
        verify(readingRepository).save(any());
        verify(meterRepository).save(mockMeter);
        verify(messagingTemplate).convertAndSend(anyString(), any(Object.class));
    }

    @Test
    @DisplayName("Close monthly billing cycle computes tiered slab amount")
    void testCloseBillingCycle() {
        when(meterRepository.findById(10L)).thenReturn(Optional.of(mockMeter));
        when(summaryRepository.findByMeterIdAndBillingMonth(10L, "2026-10")).thenReturn(Optional.empty());
        when(summaryRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        // Advance reading to 250 kWh (150 kWh consumed in month)
        mockMeter.setLastReading(new BigDecimal("250.0"));

        MeterBillingCycleDto bill = service.closeMonthlyBillingCycle(10L, "2026-10");

        assertNotNull(bill);
        assertEquals("2026-10", bill.getBillingMonth());
        assertEquals(MeterBillingStatus.PENDING_SYNC, bill.getBillingStatus());
    }
}
