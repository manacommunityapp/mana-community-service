package com.manacommunity.api.serviceplatform.review.engine;

import com.manacommunity.api.serviceplatform.review.entity.ServiceReview;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Component
public class ReviewEngine {

    public double calculateWeightedRating(List<ServiceReview> reviews) {
        if (reviews == null || reviews.isEmpty()) {
            return 0.0;
        }
        double sum = 0.0;
        for (ServiceReview r : reviews) {
            sum += r.getRating();
        }
        double avg = sum / reviews.size();
        return BigDecimal.valueOf(avg).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }

    public boolean validateRatingRange(int rating) {
        return rating >= 1 && rating <= 5;
    }
}
