package com.manacommunity.api.iot.mesh.controller;

import com.manacommunity.api.iot.mesh.dto.IoTMeshDtos.*;
import com.manacommunity.api.iot.mesh.entity.IoTSensor;
import com.manacommunity.api.iot.mesh.entity.SensorAlertConfig;
import com.manacommunity.api.iot.mesh.entity.SensorAnomalyEvent;
import com.manacommunity.api.iot.mesh.service.IoTMeshService;
import com.manacommunity.api.platform.web.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/iot")
@RequiredArgsConstructor
public class IoTMeshController {

    private final IoTMeshService iotService;

    // ── Sensor registry ───────────────────────────────────────────────────

    @GetMapping("/sensors")
    public ResponseEntity<ApiResponse<List<IoTSensor>>> getSensors(
            @RequestParam Long communityId,
            @RequestParam(required = false) String type) {
        return ResponseEntity.ok(ApiResponse.ok(iotService.getSensors(communityId, type)));
    }

    @PostMapping("/sensors")
    public ResponseEntity<ApiResponse<IoTSensor>> registerSensor(
            @RequestBody RegisterSensorRequest req) {
        return ResponseEntity.ok(ApiResponse.ok(iotService.registerSensor(req)));
    }

    @PutMapping("/sensors/{sensorId}")
    public ResponseEntity<ApiResponse<IoTSensor>> updateSensor(
            @PathVariable Long sensorId,
            @RequestBody RegisterSensorRequest req) {
        return ResponseEntity.ok(ApiResponse.ok(iotService.updateSensor(sensorId, req)));
    }

    // ── Telemetry ─────────────────────────────────────────────────────────

    @PostMapping("/sensors/{sensorId}/reading")
    public ResponseEntity<ApiResponse<TelemetryPoint>> ingestReading(
            @PathVariable Long sensorId,
            @RequestBody IngestReadingRequest req) {
        TelemetryPoint point = iotService.ingestReading(sensorId, req.value(), req.readingAt(), req.quality());
        return ResponseEntity.ok(ApiResponse.ok(point));
    }

    @PostMapping("/sensors/{sensorId}/poll")
    public ResponseEntity<ApiResponse<BigDecimal>> pollSensor(@PathVariable Long sensorId) {
        return ResponseEntity.ok(ApiResponse.ok(iotService.pollSensor(sensorId)));
    }

    @GetMapping("/sensors/{sensorId}/trend")
    public ResponseEntity<ApiResponse<List<TelemetryPoint>>> getTrend(
            @PathVariable Long sensorId,
            @RequestParam(defaultValue = "24") int hours) {
        return ResponseEntity.ok(ApiResponse.ok(iotService.getTelemetryTrend(sensorId, hours)));
    }

    @GetMapping("/sensors/{sensorId}/aggregates")
    public ResponseEntity<ApiResponse<List<DailyAggregate>>> getAggregates(
            @PathVariable Long sensorId,
            @RequestParam(defaultValue = "30") int days) {
        return ResponseEntity.ok(ApiResponse.ok(iotService.getDailyAggregates(sensorId, days)));
    }

    // ── Alert configs ─────────────────────────────────────────────────────

    @PostMapping("/sensors/{sensorId}/alert-configs")
    public ResponseEntity<ApiResponse<SensorAlertConfig>> configureAlert(
            @PathVariable Long sensorId,
            @RequestBody AlertConfigRequest req) {
        return ResponseEntity.ok(ApiResponse.ok(iotService.configureAlert(sensorId, req)));
    }

    // ── Anomaly management ────────────────────────────────────────────────

    @GetMapping("/anomalies")
    public ResponseEntity<ApiResponse<List<SensorAnomalyEvent>>> getActiveAnomalies(
            @RequestParam Long communityId) {
        return ResponseEntity.ok(ApiResponse.ok(iotService.getActiveAnomalies(communityId)));
    }

    @RequestMapping(value = "/anomalies/{anomalyId}/resolve", method = {RequestMethod.PUT, RequestMethod.POST})
    public ResponseEntity<ApiResponse<SensorAnomalyEvent>> resolveAnomaly(
            @PathVariable Long anomalyId,
            @RequestParam(required = false) Long resolvedBy,
            @org.springframework.security.core.annotation.AuthenticationPrincipal com.manacommunity.api.user.security.UserPrincipal principal) {
        Long resolverId = resolvedBy != null ? resolvedBy : (principal != null ? principal.getId() : 1L);
        return ResponseEntity.ok(ApiResponse.ok(iotService.resolveAnomaly(anomalyId, resolverId)));
    }

    // ── Emergency ─────────────────────────────────────────────────────────

    @PostMapping("/emergency")
    public ResponseEntity<ApiResponse<Void>> broadcastEmergency(
            @RequestParam Long communityId,
            @RequestBody EmergencyBroadcastRequest req) {
        iotService.broadcastEmergency(communityId, req.message(), req.severity());
        return ResponseEntity.ok(ApiResponse.ok(null));
    }
}
