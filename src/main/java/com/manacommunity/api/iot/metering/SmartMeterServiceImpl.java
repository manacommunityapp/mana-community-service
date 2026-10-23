package com.manacommunity.api.iot.metering;

import com.manacommunity.api.exception.ResourceNotFoundException;
import com.manacommunity.api.iot.metering.MeteringEnums.*;
import com.manacommunity.api.iot.metering.dto.MeteringDtos.*;
import com.manacommunity.api.iot.metering.engine.MeterPulseIngestionEngine;
import com.manacommunity.api.iot.metering.engine.UtilityTierSlabEngine;
import com.manacommunity.api.model.Community;
import com.manacommunity.api.repository.CommunityRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SmartMeterServiceImpl implements SmartMeterService {

    private final SmartMeterRepository meterRepository;
    private final MeterTelemetryReadingRepository readingRepository;
    private final MeterBillingCycleSummaryRepository summaryRepository;
    private final CommunityRepository communityRepository;
    private final MeterPulseIngestionEngine ingestionEngine;
    private final UtilityTierSlabEngine slabEngine;
    private final SimpMessagingTemplate messagingTemplate;

    @Override
    @Transactional
    public SmartMeterDto registerMeter(RegisterMeterRequest request) {
        Community community = communityRepository.findById(request.getCommunityId())
                .orElseThrow(() -> new ResourceNotFoundException("Community", request.getCommunityId()));

        SmartMeter meter = SmartMeter.builder()
                .meterSerialNumber(request.getMeterSerialNumber())
                .meterType(request.getMeterType())
                .community(community)
                .unitNumber(request.getUnitNumber())
                .blockName(request.getBlockName())
                .protocol(request.getProtocol() != null ? request.getProtocol() : MeterProtocol.MQTT)
                .pulseMultiplier(request.getPulseMultiplier() != null ? request.getPulseMultiplier() : BigDecimal.ONE)
                .ipAddress(request.getIpAddress())
                .mqttTopic(request.getMqttTopic())
                .build();

        SmartMeter saved = meterRepository.save(meter);
        return mapToDto(saved);
    }

    @Override
    @Transactional
    public IngestTelemetryResult ingestTelemetry(IngestTelemetryRequest request) {
        SmartMeter meter = meterRepository.findByMeterSerialNumber(request.getMeterSerialNumber())
                .orElseThrow(() -> new IllegalArgumentException("Smart meter not registered: " + request.getMeterSerialNumber()));

        var analysis = ingestionEngine.processPulse(
                meter,
                request.getPulseCount(),
                request.getInstantaneousFlow(),
                request.getTamperFlag()
        );

        MeterTelemetryReading reading = MeterTelemetryReading.builder()
                .meter(meter)
                .timestamp(LocalDateTime.now())
                .rawPulseCount(request.getPulseCount())
                .cumulativeConsumption(analysis.newCumulativeReading())
                .deltaConsumption(analysis.deltaConsumed())
                .instantaneousFlow(request.getInstantaneousFlow())
                .voltage(request.getVoltage())
                .current(request.getCurrent())
                .powerFactor(request.getPowerFactor())
                .tamperFlag(analysis.status() == MeterStatus.TAMPERED)
                .signalRssi(request.getSignalRssi())
                .rawPayloadJson(request.getRawPayloadJson())
                .build();

        readingRepository.save(reading);

        // Update smart meter state
        meter.setLastReading(analysis.newCumulativeReading());
        meter.setLastPulseCount(request.getPulseCount());
        meter.setLastTelemetryTime(LocalDateTime.now());
        meter.setStatus(analysis.status());
        meter.setLeakDetected(analysis.leakFlag());
        if (request.getBatteryLevel() != null) {
            meter.setBatteryLevel(request.getBatteryLevel());
        }
        meterRepository.save(meter);

        // Broadcast real-time telemetry update over WebSocket
        messagingTemplate.convertAndSend(
                "/topic/community/" + meter.getCommunity().getId() + "/metering",
                (Object) Map.of(
                        "meterSerial", meter.getMeterSerialNumber(),
                        "unitNumber", meter.getUnitNumber() != null ? meter.getUnitNumber() : "",
                        "type", meter.getMeterType().name(),
                        "lastReading", meter.getLastReading(),
                        "delta", analysis.deltaConsumed(),
                        "status", meter.getStatus().name(),
                        "leak", meter.getLeakDetected()
                )
        );

        return IngestTelemetryResult.builder()
                .meterSerialNumber(meter.getMeterSerialNumber())
                .meterType(meter.getMeterType())
                .deltaConsumed(analysis.deltaConsumed())
                .cumulativeReading(analysis.newCumulativeReading())
                .anomalyDetected(analysis.anomalyDetected())
                .alertMessage(analysis.alertMessage())
                .status(analysis.status())
                .build();
    }

    @Override
    @Transactional
    public List<IngestTelemetryResult> ingestBatch(ModbusTelemetryBatchRequest request) {
        List<IngestTelemetryResult> results = new ArrayList<>();
        if (request.getReadings() != null) {
            for (IngestTelemetryRequest r : request.getReadings()) {
                results.add(ingestTelemetry(r));
            }
        }
        return results;
    }

    @Override
    @Transactional(readOnly = true)
    public List<SmartMeterDto> getMetersForCommunity(Long communityId) {
        return meterRepository.findByCommunityId(communityId).stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Page<MeterTelemetryReading> getReadingsForMeter(Long meterId, Pageable pageable) {
        return readingRepository.findByMeterIdOrderByTimestampDesc(meterId, pageable);
    }

    @Override
    @Transactional
    public MeterBillingCycleDto closeMonthlyBillingCycle(Long meterId, String billingMonth) {
        SmartMeter meter = meterRepository.findById(meterId)
                .orElseThrow(() -> new ResourceNotFoundException("SmartMeter", meterId));

        MeterBillingCycleSummary summary = summaryRepository.findByMeterIdAndBillingMonth(meterId, billingMonth)
                .orElseGet(() -> MeterBillingCycleSummary.builder()
                        .meter(meter)
                        .community(meter.getCommunity())
                        .unitNumber(meter.getUnitNumber())
                        .meterType(meter.getMeterType())
                        .billingMonth(billingMonth)
                        .startReading(meter.getLastReading()) // Fallback start
                        .endReading(meter.getLastReading())
                        .totalUnitsConsumed(BigDecimal.ZERO)
                        .slabAmount(BigDecimal.ZERO)
                        .build());

        BigDecimal totalUnits = meter.getLastReading().subtract(summary.getStartReading()).max(BigDecimal.ZERO);
        BigDecimal slabAmount = slabEngine.calculateSlabAmount(meter.getMeterType(), totalUnits);

        summary.setEndReading(meter.getLastReading());
        summary.setTotalUnitsConsumed(totalUnits);
        summary.setSlabAmount(slabAmount);
        summary.setBillingStatus(MeterBillingStatus.PENDING_SYNC);

        MeterBillingCycleSummary saved = summaryRepository.save(summary);
        return mapToSummaryDto(saved);
    }

    @Override
    @Transactional
    public List<MeterBillingCycleDto> syncMonthlyBillingToCfbos(Long communityId, String billingMonth) {
        List<MeterBillingCycleSummary> summaries = summaryRepository.findByCommunityIdAndBillingMonth(communityId, billingMonth);
        List<MeterBillingCycleDto> dtos = new ArrayList<>();

        for (MeterBillingCycleSummary s : summaries) {
            // Simulated CFBOS invoice link
            s.setBillingStatus(MeterBillingStatus.BILLED_IN_CFBOS);
            s.setCfbosInvoiceId(1000L + s.getId());
            summaryRepository.save(s);
            dtos.add(mapToSummaryDto(s));
        }

        log.info("Synced {} meter utility charges into CFBOS for month {}", summaries.size(), billingMonth);
        return dtos;
    }

    private SmartMeterDto mapToDto(SmartMeter meter) {
        return SmartMeterDto.builder()
                .id(meter.getId())
                .meterSerialNumber(meter.getMeterSerialNumber())
                .meterType(meter.getMeterType())
                .communityId(meter.getCommunity().getId())
                .unitNumber(meter.getUnitNumber())
                .blockName(meter.getBlockName())
                .protocol(meter.getProtocol())
                .pulseMultiplier(meter.getPulseMultiplier())
                .lastReading(meter.getLastReading())
                .lastTelemetryTime(meter.getLastTelemetryTime())
                .status(meter.getStatus())
                .batteryLevel(meter.getBatteryLevel())
                .leakDetected(Boolean.TRUE.equals(meter.getLeakDetected()))
                .build();
    }

    private MeterBillingCycleDto mapToSummaryDto(MeterBillingCycleSummary s) {
        return MeterBillingCycleDto.builder()
                .id(s.getId())
                .meterId(s.getMeter().getId())
                .meterSerialNumber(s.getMeter().getMeterSerialNumber())
                .unitNumber(s.getUnitNumber())
                .meterType(s.getMeterType())
                .billingMonth(s.getBillingMonth())
                .startReading(s.getStartReading())
                .endReading(s.getEndReading())
                .totalUnitsConsumed(s.getTotalUnitsConsumed())
                .slabAmount(s.getSlabAmount())
                .cfbosInvoiceId(s.getCfbosInvoiceId())
                .billingStatus(s.getBillingStatus())
                .build();
    }
}
