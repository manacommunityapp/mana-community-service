package com.manacommunity.api.service;

import com.manacommunity.api.dto.MeterReadingRequest;
import com.manacommunity.api.dto.SmartMeterRequest;
import com.manacommunity.api.model.MeterReading;
import com.manacommunity.api.model.SmartMeter;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public interface SmartMeterService {

    List<SmartMeter> getMeters(Long communityId, String flat, String type);
    SmartMeter getMeter(Long communityId, Long id);
    SmartMeter createMeter(Long communityId, SmartMeterRequest request);

    List<MeterReading> getReadings(Long communityId, Long meterId, LocalDate from, LocalDate to);
    MeterReading addReading(Long communityId, Long meterId, MeterReadingRequest request);

    List<MeterReading> getAlerts(Long communityId);
    Map<String, Object> getCommunityUsage(Long communityId, String type, Integer month, Integer year);
}
