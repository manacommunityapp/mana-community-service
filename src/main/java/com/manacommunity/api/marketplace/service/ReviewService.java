package com.manacommunity.api.marketplace.service;

import com.manacommunity.api.exception.DuplicateResourceException;
import com.manacommunity.api.exception.InvalidInputException;
import com.manacommunity.api.exception.ResourceNotFoundException;
import com.manacommunity.api.exception.UnauthorizedActionException;
import com.manacommunity.api.marketplace.dto.ReviewRequest;
import com.manacommunity.api.marketplace.dto.ReviewResponse;
import com.manacommunity.api.marketplace.entity.Listing;
import com.manacommunity.api.marketplace.entity.ListingReview;
import com.manacommunity.api.marketplace.repository.ListingRepository;
import com.manacommunity.api.marketplace.repository.ListingReviewRepository;
import com.manacommunity.api.security.AuditAction;
import com.manacommunity.api.security.AuditModule;
import com.manacommunity.api.security.AuditService;
import com.manacommunity.api.user.model.AppUser;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class ReviewService {

    private final ListingReviewRepository reviewRepo;
    private final ListingRepository listingRepo;
    private final AuditService auditService;

    @Transactional(readOnly = true)
    public Page<ReviewResponse> getListingReviews(Long listingId, Pageable pageable) {
        return reviewRepo.findByListingIdOrderByCreatedAtDesc(listingId, pageable)
                .map(this::toResponse);
    }

    @Transactional
    public ReviewResponse createReview(ReviewRequest req, AppUser reviewer) {
        Listing listing = listingRepo.findById(req.getListingId())
                .orElseThrow(() -> new ResourceNotFoundException("Listing", req.getListingId()));

        if (listing.getSeller().getId().equals(reviewer.getId())) {
            throw new InvalidInputException("You cannot review your own listing");
        }

        if (reviewRepo.existsByListingIdAndReviewerId(req.getListingId(), reviewer.getId())) {
            throw new DuplicateResourceException("Review", "listing", String.valueOf(req.getListingId()));
        }

        ListingReview review = ListingReview.builder()
                .listing(listing)
                .reviewer(reviewer)
                .rating(req.getRating())
                .comment(req.getComment())
                .build();

        ListingReview saved = reviewRepo.save(review);
        auditService.record(AuditAction.PRODUCT_UPDATED, AuditModule.MARKETPLACE,
                "ListingReview", String.valueOf(saved.getId()));
        return toResponse(saved);
    }

    @Transactional
    public ReviewResponse addSellerReply(Long reviewId, String reply, Long sellerId) {
        ListingReview review = reviewRepo.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Review", reviewId));

        if (!review.getListing().getSeller().getId().equals(sellerId)) {
            throw new UnauthorizedActionException("Only the listing seller can reply to reviews");
        }

        review.setSellerReply(reply);
        ListingReview saved = reviewRepo.save(review);
        auditService.record(AuditAction.PRODUCT_UPDATED, AuditModule.MARKETPLACE,
                "ListingReview", String.valueOf(saved.getId()));
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getListingStats(Long listingId) {
        Double avg = reviewRepo.findAverageRatingByListingId(listingId);
        long total = reviewRepo.countByListingId(listingId);
        return Map.of(
                "averageRating", avg != null ? avg : 0.0,
                "totalReviews", total
        );
    }

    @Transactional(readOnly = true)
    public Double getSellerRating(Long sellerId) {
        return reviewRepo.findAverageRatingBySellerId(sellerId);
    }

    private ReviewResponse toResponse(ListingReview r) {
        AppUser rev = r.getReviewer();
        return ReviewResponse.builder()
                .id(r.getId())
                .listingId(r.getListing().getId())
                .listingTitle(r.getListing().getTitle())
                .reviewer(ReviewResponse.ReviewerRef.builder()
                        .id(rev.getId())
                        .fullName(rev.getFullName())
                        .verified("APPROVED".equalsIgnoreCase(rev.getKycStatus()))
                        .build())
                .rating(r.getRating())
                .comment(r.getComment())
                .sellerReply(r.getSellerReply())
                .createdAt(r.getCreatedAt())
                .build();
    }
}
