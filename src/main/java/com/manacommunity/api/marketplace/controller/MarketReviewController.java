package com.manacommunity.api.marketplace.controller;

import com.manacommunity.api.marketplace.dto.MarketReviewRequest;
import com.manacommunity.api.marketplace.dto.MarketReviewResponse;
import com.manacommunity.api.marketplace.service.MarketReviewService;
import com.manacommunity.api.user.model.AppUser;
import com.manacommunity.api.user.security.UserPrincipal;
import com.manacommunity.api.user.service.LoggedInUserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping({"/api/marketplace/reviews", "/api/v1/marketplace/reviews"})
@RequiredArgsConstructor
public class MarketReviewController {

    private final MarketReviewService reviewService;
    private final LoggedInUserService loggedInUserService;

    @GetMapping("/listing/{listingId}")
    public ResponseEntity<List<MarketReviewResponse>> getListingReviews(@PathVariable Long listingId) {
        return ResponseEntity.ok(reviewService.getReviewsForListing(listingId));
    }

    @GetMapping("/seller/{sellerId}")
    public ResponseEntity<List<MarketReviewResponse>> getSellerReviews(@PathVariable Long sellerId) {
        return ResponseEntity.ok(reviewService.getReviewsForSeller(sellerId));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('View Marketplace')")
    public ResponseEntity<MarketReviewResponse> addReview(
            @Valid @RequestBody MarketReviewRequest.Create req,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(reviewService.addReview(req, user));
    }

    @PostMapping("/{id}/reply")
    @PreAuthorize("hasAuthority('View Marketplace')")
    public ResponseEntity<MarketReviewResponse> replyToReview(
            @PathVariable Long id,
            @Valid @RequestBody MarketReviewRequest.Reply req,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(reviewService.replyToReview(id, req, user.getId()));
    }
}
