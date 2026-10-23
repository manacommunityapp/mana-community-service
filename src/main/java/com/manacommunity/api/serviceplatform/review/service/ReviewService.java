package com.manacommunity.api.serviceplatform.review.service;

import com.manacommunity.api.exception.ResourceNotFoundException;
import com.manacommunity.api.user.model.AppUser;
import com.manacommunity.api.user.repository.AppUserRepository;
import com.manacommunity.api.serviceplatform.entity.ServiceProvider;
import com.manacommunity.api.serviceplatform.entity.WorkOrder;
import com.manacommunity.api.serviceplatform.repository.ServiceProviderRepository;
import com.manacommunity.api.serviceplatform.repository.WorkOrderRepository;
import com.manacommunity.api.serviceplatform.review.dto.CreateReviewRequest;
import com.manacommunity.api.serviceplatform.review.dto.ReviewResponse;
import com.manacommunity.api.serviceplatform.review.engine.ReviewEngine;
import com.manacommunity.api.serviceplatform.review.entity.ServiceReview;
import com.manacommunity.api.serviceplatform.review.repository.ServiceReviewRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReviewService {

    private final ServiceReviewRepository reviewRepository;
    private final WorkOrderRepository workOrderRepository;
    private final ServiceProviderRepository providerRepository;
    private final AppUserRepository userRepository;
    private final ReviewEngine engine;

    @Transactional
    public ReviewResponse createReview(CreateReviewRequest request) {
        if (!engine.validateRatingRange(request.getRating())) {
            throw new IllegalArgumentException("Rating must be between 1 and 5");
        }

        WorkOrder workOrder = workOrderRepository.findById(request.getWorkOrderId())
                .orElseThrow(() -> new ResourceNotFoundException("WorkOrder", request.getWorkOrderId()));

        AppUser reviewer = userRepository.findById(request.getReviewerId())
                .orElseThrow(() -> new ResourceNotFoundException("AppUser", request.getReviewerId()));

        ServiceProvider provider = workOrder.getProvider();

        ServiceReview review = ServiceReview.builder()
                .workOrder(workOrder)
                .provider(provider)
                .reviewer(reviewer)
                .rating(request.getRating())
                .qualityRating(request.getQualityRating())
                .punctualityRating(request.getPunctualityRating())
                .behaviorRating(request.getBehaviorRating())
                .comment(request.getComment())
                .verifiedResident(true)
                .build();

        ServiceReview saved = reviewRepository.save(review);

        // Update provider cached rating
        Double avgRating = reviewRepository.calculateAverageRating(provider.getId());
        if (avgRating != null) {
            provider.setAvgRating(BigDecimal.valueOf(avgRating));
            providerRepository.save(provider);
        }

        return mapToResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<ReviewResponse> getReviewsByProvider(Long providerId) {
        return reviewRepository.findByProviderId(providerId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public ReviewResponse respondToReview(Long reviewId, String responseText) {
        ServiceReview review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("ServiceReview", reviewId));
        review.setProviderResponse(responseText);
        review.setResponseAt(LocalDateTime.now());
        return mapToResponse(reviewRepository.save(review));
    }

    private ReviewResponse mapToResponse(ServiceReview r) {
        return ReviewResponse.builder()
                .id(r.getId())
                .workOrderId(r.getWorkOrder().getId())
                .providerId(r.getProvider().getId())
                .providerName(r.getProvider().getBusinessName())
                .reviewerId(r.getReviewer().getId())
                .reviewerName(r.getReviewer().getFullName())
                .rating(r.getRating())
                .qualityRating(r.getQualityRating())
                .punctualityRating(r.getPunctualityRating())
                .behaviorRating(r.getBehaviorRating())
                .comment(r.getComment())
                .providerResponse(r.getProviderResponse())
                .responseAt(r.getResponseAt())
                .verifiedResident(r.isVerifiedResident())
                .createdAt(r.getCreatedAt())
                .build();
    }
}
