package com.manacommunity.api.marketplace.controller;

import com.manacommunity.api.marketplace.dto.ReviewRequest;
import com.manacommunity.api.marketplace.dto.ReviewResponse;
import com.manacommunity.api.marketplace.service.ReviewService;
import com.manacommunity.api.user.model.AppUser;
import com.manacommunity.api.user.security.UserPrincipal;
import com.manacommunity.api.user.service.LoggedInUserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/marketplace/reviews")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;
    private final LoggedInUserService loggedInUserService;

    @GetMapping("/listing/{listingId}")
    @PreAuthorize("hasAuthority('View Marketplace')")
    public ResponseEntity<Page<ReviewResponse>> getListingReviews(
            @PathVariable Long listingId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        PageRequest pageable = PageRequest.of(page, Math.min(size, 50), Sort.by(Sort.Direction.DESC, "createdAt"));
        return ResponseEntity.ok(reviewService.getListingReviews(listingId, pageable));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('View Marketplace')")
    public ResponseEntity<ReviewResponse> createReview(
            @Valid @RequestBody ReviewRequest req,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(reviewService.createReview(req, user));
    }

    @PutMapping("/{reviewId}/reply")
    @PreAuthorize("hasAuthority('Create Listing')")
    public ResponseEntity<ReviewResponse> addSellerReply(
            @PathVariable Long reviewId,
            @RequestParam String reply,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(reviewService.addSellerReply(reviewId, reply, user.getId()));
    }

    @GetMapping("/listing/{listingId}/stats")
    @PreAuthorize("hasAuthority('View Marketplace')")
    public ResponseEntity<Map<String, Object>> getStats(@PathVariable Long listingId) {
        return ResponseEntity.ok(reviewService.getListingStats(listingId));
    }

    @GetMapping("/seller/{sellerId}/rating")
    @PreAuthorize("hasAuthority('View Marketplace')")
    public ResponseEntity<Double> getSellerRating(@PathVariable Long sellerId) {
        return ResponseEntity.ok(reviewService.getSellerRating(sellerId));
    }
}
