package com.manacommunity.api.service.impl;

import com.manacommunity.api.dto.MeterReadingRequest;
import com.manacommunity.api.dto.SmartMeterRequest;
import com.manacommunity.api.model.MeterReading;
import com.manacommunity.api.model.SmartMeter;
import com.manacommunity.api.repository.MeterReadingRepository;
import com.manacommunity.api.repository.SmartMeterRepository;
import com.manacommunity.api.service.SmartMeterService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class SmartMeterServiceImpl implements SmartMeterService {

    private static final BigDecimal HIGH_CONSUMPTION_THRESHOLD = new BigDecimal("500");

    private final SmartMeterRepository meterRepository;
    private final MeterReadingRepository readingRepository;

    @Override
    public List<SmartMeter> getMeters(Long communityId, String flat, String type) {
        if (flat != null && !flat.isBlank()) {
            return meterRepository.findByCommunityIdAndFlatNumberOrderByMeterType(communityId, flat);
        }
        if (type != null && !type.isBlank()) {
            return meterRepository.findByCommunityIdAndMeterTypeOrderByFlatNumber(
                    communityId, SmartMeter.MeterType.valueOf(type.toUpperCase()));
        }
        return meterRepository.findByCommunityIdOrderByFlatNumber(communityId);
    }

    @Override
    public SmartMeter getMeter(Long communityId, Long id) {
        return meterRepository.findByIdAndCommunityId(id, communityId)
                .orElseThrow(() -> new IllegalArgumentException("Meter not found: " + id));
    }

    @Override
    @Transactional
    public SmartMeter createMeter(Long communityId, SmartMeterRequest request) {
        SmartMeter meter = SmartMeter.builder()
                .communityId(communityId)
                .flatNumber(request.flatNumber())
                .meterType(SmartMeter.MeterType.valueOf(request.meterType().toUpperCase()))
                .meterSerial(request.meterSerial())
                .location(request.location())
                .build();
        return meterRepository.save(meter);
    }

    @Override
    public List<MeterReading> getReadings(Long communityId, Long meterId, LocalDate from, LocalDate to) {
        // verify meter belongs to community
        meterRepository.findByIdAndCommunityId(meterId, communityId)
                .orElseThrow(() -> new IllegalArgumentException("Meter not found: " + meterId));

        if (from != null && to != null) {
            return readingRepository.findByMeterIdAndReadingDateBetweenOrderByReadingDateDesc(meterId, from, to);
        }
        return readingRepository.findByMeterIdOrderByReadingDateDesc(meterId);
    }

    @Override
    @Transactional
    public MeterReading addReading(Long communityId, Long meterId, MeterReadingRequest request) {
        SmartMeter meter = meterRepository.findByIdAndCommunityId(meterId, communityId)
                .orElseThrow(() -> new IllegalArgumentException("Meter not found: " + meterId));

        MeterReading reading = MeterReading.builder()
                .meter(meter)
                .readingValue(request.readingValue())
                .readingDate(request.readingDate())
                .consumption(request.consumption())
                .unit(request.unit())
                .billedAmount(request.billedAmount())
                .build();

        MeterReading saved = readingRepository.save(reading);

        // update meter's last reading
        meter.setLastReading(request.readingValue());
        meter.setLastReadingDate(request.readingDate());
        meterRepository.save(meter);

        return saved;
    }

    @Override
    public List<MeterReading> getAlerts(Long communityId) {
        return readingRepository.findHighConsumptionAlerts(communityId, HIGH_CONSUMPTION_THRESHOLD);
    }

    @Override
    public Map<String, Object> getCommunityUsage(Long communityId, String type, Integer month, Integer year) {
        LocalDate now = LocalDate.now();
        int m = month != null ? month : now.getMonthValue();
        int y = year != null ? year : now.getYear();

        SmartMeter.MeterType meterType = type != null
                ? SmartMeter.MeterType.valueOf(type.toUpperCase())
                : SmartMeter.MeterType.ELECTRICITY;

        BigDecimal totalConsumption = readingRepository.sumConsumptionByCommunityAndTypeAndMonth(
                communityId, meterType, m, y);

        Map<String, Object> usage = new HashMap<>();
        usage.put("type", meterType);
        usage.put("month", m);
        usage.put("year", y);
        usage.put("totalConsumption", totalConsumption);
        return usage;
    }
}
