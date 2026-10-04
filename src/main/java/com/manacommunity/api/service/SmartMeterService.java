package com.manacommunity.api.service;

import com.manacommunity.api.model.SmartMeter;
import com.manacommunity.api.model.UtilityConsumption;
import com.manacommunity.api.repository.SmartMeterRepository;
import com.manacommunity.api.repository.UtilityConsumptionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;

@Service
public class SmartMeterService {

    @Autowired
    private SmartMeterRepository meterRepository;

    @Autowired
    private UtilityConsumptionRepository consumptionRepository;

    public List<SmartMeter> getMetersForUnit(Long unitId) {
        return meterRepository.findByUnitId(unitId);
    }

    public UtilityConsumption getConsumptionSummary(Long unitId, String month) {
        String cycleMonth = month != null ? month :
                YearMonth.now().format(DateTimeFormatter.ofPattern("yyyy-MM"));
        return consumptionRepository.findByUnitIdAndCycleMonth(unitId, cycleMonth)
                .orElse(UtilityConsumption.builder()
                        .unitId(unitId)
                        .cycleMonth(cycleMonth)
                        .build());
    }

    public List<SmartMeter> getCommunityMeters(Long communityId, String type) {
        if (type != null && !type.isEmpty()) {
            return meterRepository.findBySocietyIdAndMeterType(communityId, type);
        }
        return meterRepository.findBySocietyId(communityId);
    }

    public List<Object> getMeterHistory(Long meterId, int days) {
        // Placeholder — real implementation would query time-series data
        return Collections.emptyList();
    }

    @Transactional
    public SmartMeter submitReading(Long meterId, SmartMeter reading) {
        return meterRepository.findById(meterId).map(meter -> {
            meter.setCurrentReading(reading.getCurrentReading());
            meter.setLastReadingAt(LocalDateTime.now());
            if (reading.getBurstLeakDetected() != null) {
                meter.setBurstLeakDetected(reading.getBurstLeakDetected());
            }
            return meterRepository.save(meter);
        }).orElseThrow(() -> new RuntimeException("Meter not found: " + meterId));
    }
}
