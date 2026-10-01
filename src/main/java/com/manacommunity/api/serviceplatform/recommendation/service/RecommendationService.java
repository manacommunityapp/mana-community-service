package com.manacommunity.api.serviceplatform.recommendation.service;

import com.manacommunity.api.serviceplatform.entity.ServiceProvider;
import com.manacommunity.api.serviceplatform.recommendation.dto.ProviderMatchScoreDto;
import com.manacommunity.api.serviceplatform.recommendation.dto.RecommendedServiceDto;
import com.manacommunity.api.serviceplatform.recommendation.engine.RecommendationEngine;
import com.manacommunity.api.serviceplatform.repository.ServiceProviderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class RecommendationService {

    private final ServiceProviderRepository providerRepository;
    private final RecommendationEngine engine;

    @Transactional(readOnly = true)
    public List<ProviderMatchScoreDto> recommendProvidersForCategory(Long categoryId) {
        List<ServiceProvider> providers = providerRepository.findVerifiedActiveProvidersByCategory(categoryId);
        return engine.rankProviders(providers);
    }

    @Transactional(readOnly = true)
    public List<RecommendedServiceDto> getSeasonalRecommendations(Long communityId) {
        List<RecommendedServiceDto> recommendations = new ArrayList<>();
        recommendations.add(RecommendedServiceDto.builder()
                .categoryId(1L)
                .categoryName("Air Conditioner Servicing")
                .reason("Pre-summer maintenance recommended for optimal cooling and energy efficiency.")
                .confidenceScore(0.92)
                .build());
        recommendations.add(RecommendedServiceDto.builder()
                .categoryId(2L)
                .categoryName("Pest Control & Sanitization")
                .reason("Seasonal pest control recommended before monsoon.")
                .confidenceScore(0.85)
                .build());
        return recommendations;
    }
}
