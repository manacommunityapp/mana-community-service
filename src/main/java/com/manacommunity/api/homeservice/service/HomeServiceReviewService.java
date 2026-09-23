package com.manacommunity.api.homeservice.service;

import com.manacommunity.api.homeservice.dto.HomeServiceReviewRequest;
import com.manacommunity.api.homeservice.model.entity.HomeServiceReviewEntity;
import com.manacommunity.api.homeservice.model.entity.HomeServiceWorkerEntity;
import com.manacommunity.api.homeservice.repository.HomeServiceReviewRepository;
import com.manacommunity.api.homeservice.repository.HomeServiceWorkerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service("homeServiceReviewService")
@RequiredArgsConstructor
public class HomeServiceReviewService {
    private final HomeServiceReviewRepository reviewRepository;
    private final HomeServiceWorkerRepository workerRepository;

    @Transactional
    public HomeServiceReviewEntity submitReview(HomeServiceReviewRequest req) {
        HomeServiceReviewEntity review = HomeServiceReviewEntity.builder()
                .id(UUID.randomUUID().toString())
                .bookingId(req.getBookingId())
                .reviewerUserId(req.getReviewerUserId())
                .reviewerName(req.getReviewerName())
                .reviewerFlatInfo(req.getReviewerFlatInfo())
                .revieweeWorkerId(req.getRevieweeWorkerId())
                .rating(req.getRating())
                .workQuality(req.getWorkQuality())
                .punctuality(req.getPunctuality())
                .behaviour(req.getBehaviour())
                .reliability(req.getReliability())
                .reviewComment(req.getComment())
                .verifiedResident(true)
                .createdAt(LocalDateTime.now())
                .build();

        review = reviewRepository.save(review);

        List<HomeServiceReviewEntity> reviews = reviewRepository.findByRevieweeWorkerIdOrderByCreatedAtDesc(req.getRevieweeWorkerId());
        if (!reviews.isEmpty()) {
            double avg = reviews.stream().mapToDouble(r -> r.getRating().doubleValue()).average().orElse(5.0);
            workerRepository.findById(req.getRevieweeWorkerId()).ifPresent(worker -> {
                worker.setRating(BigDecimal.valueOf(avg).setScale(2, RoundingMode.HALF_UP));
                worker.setTotalReviews(reviews.size());
                workerRepository.save(worker);
            });
        }

        return review;
    }

    public List<HomeServiceReviewEntity> getReviewsForWorker(String workerId) {
        return reviewRepository.findByRevieweeWorkerIdOrderByCreatedAtDesc(workerId);
    }
}
