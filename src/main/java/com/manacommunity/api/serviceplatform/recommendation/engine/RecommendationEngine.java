package com.manacommunity.api.serviceplatform.recommendation.engine;

import com.manacommunity.api.serviceplatform.entity.ServiceProvider;
import com.manacommunity.api.serviceplatform.recommendation.dto.ProviderMatchScoreDto;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Component
public class RecommendationEngine {

    public ProviderMatchScoreDto scoreProvider(ServiceProvider provider, double distanceKm) {
        double rating = provider.getAvgRating() != null ? provider.getAvgRating().doubleValue() : 3.0;
        double ratingScore = (rating / 5.0) * 50.0;

        int completedJobs = provider.getTotalJobsCompleted() != null ? provider.getTotalJobsCompleted() : 0;
        double jobsScore = Math.min(30.0, completedJobs * 1.5);

        double distScore = Math.max(0.0, 20.0 - (distanceKm * 2.0));

        double total = ratingScore + jobsScore + distScore;
        double overallScore = BigDecimal.valueOf(total).setScale(2, RoundingMode.HALF_UP).doubleValue();

        String reason = String.format("Rating: %.1f/5, %d completed jobs, ~%.1f km away",
                rating, completedJobs, distanceKm);

        return ProviderMatchScoreDto.builder()
                .providerId(provider.getId())
                .providerName(provider.getBusinessName())
                .overallScore(overallScore)
                .ratingScore(ratingScore)
                .completedJobsScore(jobsScore)
                .distanceScore(distScore)
                .reason(reason)
                .build();
    }

    public List<ProviderMatchScoreDto> rankProviders(List<ServiceProvider> providers) {
        List<ProviderMatchScoreDto> scores = new ArrayList<>();
        for (ServiceProvider p : providers) {
            scores.add(scoreProvider(p, 5.0));
        }
        scores.sort(Comparator.comparingDouble(ProviderMatchScoreDto::getOverallScore).reversed());
        return scores;
    }
}
