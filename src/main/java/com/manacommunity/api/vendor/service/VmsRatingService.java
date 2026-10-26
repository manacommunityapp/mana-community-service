package com.manacommunity.api.vendor.service;

import com.manacommunity.api.exception.ResourceNotFoundException;
import com.manacommunity.api.model.Community;
import com.manacommunity.api.user.model.AppUser;
import com.manacommunity.api.vendor.dto.RatingRequest;
import com.manacommunity.api.vendor.dto.RatingResponse;
import com.manacommunity.api.vendor.entity.*;
import com.manacommunity.api.vendor.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class VmsRatingService {

    private final VmsRatingRepository ratingRepo;
    private final VmsVendorRepository vendorRepo;
    private final VmsVendorPerformanceRepository performanceRepo;

    @Transactional(readOnly = true)
    public Page<RatingResponse> getVendorRatings(Long vendorId, Long communityId, Pageable pageable) {
        vendorRepo.findByIdAndCommunityId(vendorId, communityId)
                .orElseThrow(() -> new ResourceNotFoundException("Vendor", vendorId));
        return ratingRepo.findByVendorIdAndStatus(vendorId, "PUBLISHED", pageable)
                .map(this::toResponse);
    }

    @Transactional
    public RatingResponse submitRating(RatingRequest req, AppUser user, Community community) {
        VmsVendor vendor = vendorRepo.findById(req.getVendorId())
                .orElseThrow(() -> new ResourceNotFoundException("Vendor", req.getVendorId()));

        VmsRating rating = VmsRating.builder()
                .vendor(vendor)
                .user(user)
                .overallRating(req.getOverallRating())
                .comment(req.getComment())
                .isAnonymous(req.getIsAnonymous() != null ? req.getIsAnonymous() : false)
                .community(community)
                .build();

        if (req.getBookingId() != null) {
            ratingRepo.findByVendorIdAndUserIdAndBookingId(req.getVendorId(), user.getId(), req.getBookingId())
                    .ifPresent(existing -> { throw new IllegalStateException("Already rated this booking"); });
        }

        VmsRating saved = ratingRepo.save(rating);
        updateVendorRatingStats(vendor);
        return toResponse(saved);
    }

    @Transactional
    public RatingResponse replyToRating(Long ratingId, String reply, AppUser user) {
        VmsVendor vendor = vendorRepo.findByUserIdAndCommunityId(user.getId(), user.getCommunity().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Vendor", "userId", user.getId().toString()));
        VmsRating rating = ratingRepo.findByIdAndVendorId(ratingId, vendor.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Vendor rating", ratingId));
        rating.setVendorReply(reply);
        rating.setVendorRepliedAt(java.time.LocalDateTime.now());
        return toResponse(ratingRepo.save(rating));
    }

    @Transactional
    public RatingResponse moderate(Long ratingId, String moderationStatus, Long communityId) {
        VmsRating rating = ratingRepo.findById(ratingId)
                .orElseThrow(() -> new ResourceNotFoundException("Vendor rating", ratingId));
        if (!rating.getCommunity().getId().equals(communityId)) throw new ResourceNotFoundException("Vendor rating", ratingId);
        String normalized = moderationStatus == null ? "" : moderationStatus.toUpperCase();
        rating.setStatus(switch (normalized) {
            case "APPROVED", "PUBLISHED" -> "PUBLISHED";
            case "REJECTED", "HIDDEN" -> "HIDDEN";
            case "FLAGGED" -> "FLAGGED";
            default -> throw new IllegalArgumentException("Unsupported moderation status: " + moderationStatus);
        });
        VmsRating saved = ratingRepo.save(rating);
        updateVendorRatingStats(saved.getVendor());
        return toResponse(saved);
    }

    @Transactional
    public void report(Long ratingId, String reason, AppUser user) {
        VmsRating rating = ratingRepo.findById(ratingId)
                .orElseThrow(() -> new ResourceNotFoundException("Vendor rating", ratingId));
        if (!rating.getCommunity().getId().equals(user.getCommunity().getId())) throw new ResourceNotFoundException("Vendor rating", ratingId);
        rating.setReported(true);
        rating.setReportReason(reason);
        ratingRepo.save(rating);
    }

    private void updateVendorRatingStats(VmsVendor vendor) {
        BigDecimal avgRating = ratingRepo.findAverageRatingByVendorId(vendor.getId());
        long totalRatings = ratingRepo.countByVendorIdAndStatus(vendor.getId(), "PUBLISHED");
        vendor.setAverageRating(avgRating != null ? avgRating : BigDecimal.ZERO);
        vendor.setTotalRatings((int) totalRatings);
        vendorRepo.save(vendor);
    }

    private RatingResponse toResponse(VmsRating r) {
        return RatingResponse.builder()
                .id(r.getId())
                .vendorId(r.getVendor().getId())
                .user(r.getIsAnonymous() ? null : RatingResponse.UserRef.builder()
                        .id(r.getUser().getId())
                        .fullName(r.getUser().getFullName())
                        .build())
                .bookingId(r.getBooking() != null ? r.getBooking().getId() : null)
                .overallRating(r.getOverallRating())
                .comment(r.getComment())
                .reply(r.getVendorReply())
                .repliedAt(r.getVendorRepliedAt())
                .reported(r.getReported())
                .reportReason(r.getReportReason())
                .isAnonymous(r.getIsAnonymous())
                .status(r.getStatus())
                .createdAt(r.getCreatedAt())
                .build();
    }
}
