package com.manacommunity.api.marketplace.service;

import com.manacommunity.api.exception.InvalidInputException;
import com.manacommunity.api.exception.ResourceNotFoundException;
import com.manacommunity.api.exception.UnauthorizedActionException;
import com.manacommunity.api.marketplace.dto.MarketReviewRequest;
import com.manacommunity.api.marketplace.dto.MarketReviewResponse;
import com.manacommunity.api.marketplace.entity.MarketListing;
import com.manacommunity.api.marketplace.entity.MarketReview;
import com.manacommunity.api.marketplace.repository.MarketListingRepository;
import com.manacommunity.api.marketplace.repository.MarketReviewRepository;
import com.manacommunity.api.user.model.AppUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MarketReviewService {

    private final MarketReviewRepository reviewRepository;
    private final MarketListingRepository listingRepository;

    public List<MarketReviewResponse> getReviewsForListing(Long listingId) {
        return reviewRepository.findByListingIdOrderByCreatedAtDesc(listingId).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public List<MarketReviewResponse> getReviewsForSeller(Long sellerId) {
        return reviewRepository.findBySellerId(sellerId).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public MarketReviewResponse addReview(MarketReviewRequest.Create req, AppUser reviewer) {
        MarketListing listing = listingRepository.findById(req.getListingId())
                .orElseThrow(() -> new ResourceNotFoundException("Listing not found with id: " + req.getListingId()));

        if (listing.getSeller().getId().equals(reviewer.getId())) {
            throw new InvalidInputException("You cannot review your own listing");
        }

        MarketReview review = MarketReview.builder()
                .listing(listing)
                .reviewer(reviewer)
                .rating(req.getRating())
                .comment(req.getComment())
                .build();

        return toResponse(reviewRepository.save(review));
    }

    @Transactional
    public MarketReviewResponse replyToReview(Long reviewId, MarketReviewRequest.Reply req, Long currentUserId) {
        MarketReview review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Review not found with id: " + reviewId));

        if (!review.getListing().getSeller().getId().equals(currentUserId)) {
            throw new UnauthorizedActionException("Only the seller can reply to reviews on their listing");
        }

        review.setSellerReply(req.getSellerReply());
        return toResponse(reviewRepository.save(review));
    }

    private MarketReviewResponse toResponse(MarketReview r) {
        return MarketReviewResponse.builder()
                .id(r.getId())
                .listingId(r.getListing().getId())
                .reviewerName(r.getReviewer().getFullName() != null ? r.getReviewer().getFullName() : r.getReviewer().getUsername())
                .reviewerId(r.getReviewer().getId())
                .rating(r.getRating())
                .comment(r.getComment())
                .sellerReply(r.getSellerReply())
                .createdAt(r.getCreatedAt())
                .updatedAt(r.getUpdatedAt())
                .build();
    }
}
