package com.manacommunity.api.cfbos.penalty.controller;

import com.manacommunity.api.cfbos.penalty.dto.PenaltyDto;
import com.manacommunity.api.cfbos.penalty.entity.PenaltyConfig;
import com.manacommunity.api.cfbos.penalty.service.PenaltyService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/cfbos/penalties")
@RequiredArgsConstructor
public class PenaltyController {

    private final PenaltyService penaltyService;

    @PostMapping("/assess/invoice/{invoiceId}")
    public ResponseEntity<PenaltyDto> assess(@PathVariable Long invoiceId) {
        return penaltyService.assessPenalty(invoiceId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.noContent().build());
    }

    @GetMapping("/resident/{residentId}")
    public ResponseEntity<List<PenaltyDto>> getByResident(@PathVariable Long residentId) {
        return ResponseEntity.ok(penaltyService.getPenaltiesForResident(residentId));
    }

    @GetMapping("/config")
    public ResponseEntity<PenaltyConfig> getConfig() {
        return penaltyService.getActiveConfig()
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/config")
    public ResponseEntity<PenaltyConfig> saveConfig(@RequestBody PenaltyConfig config) {
        return ResponseEntity.ok(penaltyService.saveConfig(config));
    }
}
