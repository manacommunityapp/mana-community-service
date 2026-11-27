package com.manacommunity.api.homeservices.service;

import com.manacommunity.api.homeservices.entity.HsBookingRating;
import com.manacommunity.api.homeservices.repository.HsBookingRatingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class HsRatingService {

    private final HsBookingRatingRepository ratingRepo;
    private final HsWorkerService workerService;

    @Transactional
    public HsBookingRating rate(HsBookingRating rating) {
        if (rating.getOverall() < 1 || rating.getOverall() > 5)
            throw new IllegalArgumentException("Rating must be 1-5");
        if (ratingRepo.findByBookingId(rating.getBookingId()).isPresent())
            throw new IllegalStateException("Booking already rated");
        HsBookingRating saved = ratingRepo.save(rating);
        workerService.refreshWorkerRating(rating.getWorkerId());
        return saved;
    }

    public List<HsBookingRating> workerRatings(Long workerId) {
        return ratingRepo.findByWorkerId(workerId);
    }
}
