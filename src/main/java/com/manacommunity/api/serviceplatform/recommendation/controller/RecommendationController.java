package com.manacommunity.api.serviceplatform.recommendation.controller;

import com.manacommunity.api.serviceplatform.recommendation.dto.ProviderMatchScoreDto;
import com.manacommunity.api.serviceplatform.recommendation.dto.RecommendedServiceDto;
import com.manacommunity.api.serviceplatform.recommendation.service.RecommendationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/service-platform/recommendations")
@RequiredArgsConstructor
@Tag(name = "AI Recommendations", description = "Smart provider matching and seasonal service recommendation APIs")
public class RecommendationController {

    private final RecommendationService recommendationService;

    @GetMapping("/providers/category/{categoryId}")
    @Operation(summary = "Get AI ranked provider recommendations for a service category")
    public ResponseEntity<List<ProviderMatchScoreDto>> getRecommendedProviders(@PathVariable Long categoryId) {
        return ResponseEntity.ok(recommendationService.recommendProvidersForCategory(categoryId));
    }

    @GetMapping("/seasonal")
    @Operation(summary = "Get seasonal and predictive service recommendations for a community")
    public ResponseEntity<List<RecommendedServiceDto>> getSeasonalRecommendations(
            @RequestParam(required = false, defaultValue = "1") Long communityId) {
        return ResponseEntity.ok(recommendationService.getSeasonalRecommendations(communityId));
    }
}
