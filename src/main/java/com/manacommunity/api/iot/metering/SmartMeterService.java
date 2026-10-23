package com.manacommunity.api.iot.metering;

import com.manacommunity.api.iot.metering.dto.MeteringDtos.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface SmartMeterService {
    SmartMeterDto registerMeter(RegisterMeterRequest request);
    IngestTelemetryResult ingestTelemetry(IngestTelemetryRequest request);
    List<IngestTelemetryResult> ingestBatch(ModbusTelemetryBatchRequest request);
    List<SmartMeterDto> getMetersForCommunity(Long communityId);
    Page<MeterTelemetryReading> getReadingsForMeter(Long meterId, Pageable pageable);
    MeterBillingCycleDto closeMonthlyBillingCycle(Long meterId, String billingMonth);
    List<MeterBillingCycleDto> syncMonthlyBillingToCfbos(Long communityId, String billingMonth);
}
