package com.manacommunity.api.adtech.controller;

import com.manacommunity.api.adtech.dto.AdTechDtos.*;
import com.manacommunity.api.adtech.service.AdServingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/adtech")
@RequiredArgsConstructor
public class AdTechController {

    private final AdServingService adServingService;

    @PostMapping("/ad-request")
    public ResponseEntity<AdResponseDto> requestAd(@RequestBody AdRequestDto request) {
        return adServingService.selectAd(request)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
    }

    @PostMapping("/impression")
    public ResponseEntity<Void> fireImpression(@RequestBody ImpressionEventDto event) {
        adServingService.recordImpression(event);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/click")
    public ResponseEntity<Void> fireClick(@RequestBody ClickEventDto event) {
        adServingService.recordClick(event);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/campaigns")
    public ResponseEntity<List<AdCampaignDto>> listCampaigns(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String city) {
        return ResponseEntity.ok(adServingService.listCampaigns(status, city));
    }

    @GetMapping("/campaigns/{campaignId}")
    public ResponseEntity<AdCampaignDto> getCampaign(@PathVariable String campaignId) {
        return ResponseEntity.ok(adServingService.getCampaign(campaignId));
    }

    @GetMapping("/campaigns/{campaignId}/budget")
    public ResponseEntity<BudgetPacingStateDto> getBudgetState(@PathVariable String campaignId) {
        return ResponseEntity.ok(adServingService.getBudgetState(campaignId));
    }

    @PostMapping("/campaigns/{campaignId}/pause")
    public ResponseEntity<Void> pauseCampaign(@PathVariable String campaignId) {
        adServingService.pauseCampaign(campaignId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/campaigns/{campaignId}/resume")
    public ResponseEntity<Void> resumeCampaign(@PathVariable String campaignId) {
        adServingService.resumeCampaign(campaignId);
        return ResponseEntity.ok().build();
    }
}
