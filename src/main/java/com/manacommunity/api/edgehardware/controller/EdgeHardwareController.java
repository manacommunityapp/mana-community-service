package com.manacommunity.api.edgehardware.controller;

import com.manacommunity.api.edgehardware.entity.EdgeGatewayOtaState;
import com.manacommunity.api.edgehardware.service.BarrierRelaySafetyService;
import com.manacommunity.api.edgehardware.service.BarrierRelaySafetyService.*;
import com.manacommunity.api.edgehardware.service.EdgeOtaWatchdogService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/edge")
@RequiredArgsConstructor
public class EdgeHardwareController {

    private final BarrierRelaySafetyService relaySafetyService;
    private final EdgeOtaWatchdogService otaWatchdogService;

    @PostMapping("/barrier/command")
    public ResponseEntity<BarrierCommandOutcome> executeCommand(@RequestBody BarrierCommandRequest req) {
        return ResponseEntity.ok(relaySafetyService.executeBarrierCommand(req, Instant.now()));
    }

    @PostMapping("/barrier/tailgate-check")
    public ResponseEntity<LoopCycleOutcome> evaluateLoop(@RequestBody Map<String, Object> body) {
        return ResponseEntity.ok(relaySafetyService.evaluateLoopTransition(
                body.get("controllerId").toString(),
                Boolean.parseBoolean(body.getOrDefault("previousLoopB", "false").toString()),
                Boolean.parseBoolean(body.getOrDefault("currentLoopA", "false").toString()),
                Boolean.parseBoolean(body.getOrDefault("currentLoopB", "false").toString()),
                Boolean.parseBoolean(body.getOrDefault("authorizedCycleActive", "false").toString())
        ));
    }

    @PostMapping("/ota/watchdog-evaluate")
    public ResponseEntity<EdgeGatewayOtaState> evaluateWatchdog(@RequestBody Map<String, Object> body) {
        return ResponseEntity.ok(otaWatchdogService.evaluateTrialBootWatchdog(
                body.get("controllerId").toString(),
                Boolean.parseBoolean(body.getOrDefault("serialBusHealthy", "true").toString()),
                Boolean.parseBoolean(body.getOrDefault("tlsMqttHealthy", "true").toString()),
                Long.parseLong(body.getOrDefault("secondsSinceBoot", "10").toString()),
                Boolean.parseBoolean(body.getOrDefault("crashLoopDetected", "false").toString())
        ));
    }
}
