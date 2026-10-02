package com.manacommunity.api.iot.metering;

import com.manacommunity.api.iot.metering.dto.MeteringDtos.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/iot/metering")
@RequiredArgsConstructor
public class SmartMeterController {

    private final SmartMeterService meterService;

    @PostMapping("/meters/register")
    public ResponseEntity<SmartMeterDto> registerMeter(@Valid @RequestBody RegisterMeterRequest request) {
        return ResponseEntity.ok(meterService.registerMeter(request));
    }

    @PostMapping("/telemetry/ingest")
    public ResponseEntity<IngestTelemetryResult> ingestTelemetry(@Valid @RequestBody IngestTelemetryRequest request) {
        return ResponseEntity.ok(meterService.ingestTelemetry(request));
    }

    @PostMapping("/telemetry/modbus-batch")
    public ResponseEntity<List<IngestTelemetryResult>> ingestModbusBatch(@Valid @RequestBody ModbusTelemetryBatchRequest request) {
        return ResponseEntity.ok(meterService.ingestBatch(request));
    }

    @GetMapping("/community/{communityId}")
    public ResponseEntity<List<SmartMeterDto>> getCommunityMeters(@PathVariable Long communityId) {
        return ResponseEntity.ok(meterService.getMetersForCommunity(communityId));
    }

    @GetMapping("/readings/{meterId}")
    public ResponseEntity<Page<MeterTelemetryReading>> getReadings(
            @PathVariable Long meterId,
            Pageable pageable) {
        return ResponseEntity.ok(meterService.getReadingsForMeter(meterId, pageable));
    }

    @PostMapping("/billing/{meterId}/close-month")
    public ResponseEntity<MeterBillingCycleDto> closeMonth(
            @PathVariable Long meterId,
            @RequestParam String billingMonth) {
        return ResponseEntity.ok(meterService.closeMonthlyBillingCycle(meterId, billingMonth));
    }

    @PostMapping("/billing/cfbos-sync/{communityId}")
    public ResponseEntity<List<MeterBillingCycleDto>> syncToCfbos(
            @PathVariable Long communityId,
            @RequestParam String billingMonth) {
        return ResponseEntity.ok(meterService.syncMonthlyBillingToCfbos(communityId, billingMonth));
    }
}
