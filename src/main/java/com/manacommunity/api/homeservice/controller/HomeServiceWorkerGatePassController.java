package com.manacommunity.api.homeservice.controller;

import com.manacommunity.api.homeservice.dto.HomeServiceGatePassScanRequest;
import com.manacommunity.api.homeservice.model.entity.GatePassScanLogEntity;
import com.manacommunity.api.homeservice.model.entity.WorkerGatePassRecordEntity;
import com.manacommunity.api.homeservice.service.HomeServiceWorkerGatePassService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController("homeServiceWorkerGatePassController")
@RequestMapping("/api/v1/home-services/gate-passes")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class HomeServiceWorkerGatePassController {
    private final HomeServiceWorkerGatePassService gatePassService;

    @GetMapping("/worker/{workerId}")
    public ResponseEntity<WorkerGatePassRecordEntity> getGatePass(
            @PathVariable String workerId,
            @RequestParam(defaultValue = "comm-mana-1") String communityId) {
        return ResponseEntity.ok(gatePassService.getGatePass(workerId, communityId));
    }

    @PostMapping("/scan")
    public ResponseEntity<GatePassScanLogEntity> scanGatePass(@RequestBody HomeServiceGatePassScanRequest req) {
        return ResponseEntity.ok(gatePassService.recordScan(req));
    }

    @GetMapping("/worker/{workerId}/history")
    public ResponseEntity<List<GatePassScanLogEntity>> getHistory(@PathVariable String workerId) {
        return ResponseEntity.ok(gatePassService.getScanHistory(workerId));
    }
}
