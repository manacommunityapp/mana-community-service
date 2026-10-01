package com.manacommunity.api.serviceplatform.review.controller;

import com.manacommunity.api.serviceplatform.review.dto.CreateReviewRequest;
import com.manacommunity.api.serviceplatform.review.dto.ReviewResponse;
import com.manacommunity.api.serviceplatform.review.service.ReviewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/service-platform/reviews")
@RequiredArgsConstructor
@Tag(name = "Review & Rating", description = "Service review and rating management APIs")
public class ReviewController {

    private final ReviewService reviewService;

    @PostMapping
    @Operation(summary = "Submit a review for a completed work order")
    public ResponseEntity<ReviewResponse> createReview(@Valid @RequestBody CreateReviewRequest request) {
        return new ResponseEntity<>(reviewService.createReview(request), HttpStatus.CREATED);
    }

    @GetMapping("/provider/{providerId}")
    @Operation(summary = "Get all reviews for a service provider")
    public ResponseEntity<List<ReviewResponse>> getReviewsByProvider(@PathVariable Long providerId) {
        return ResponseEntity.ok(reviewService.getReviewsByProvider(providerId));
    }

    @PostMapping("/{reviewId}/respond")
    @Operation(summary = "Service provider response to a review")
    public ResponseEntity<ReviewResponse> respondToReview(
            @PathVariable Long reviewId,
            @RequestParam String response) {
        return ResponseEntity.ok(reviewService.respondToReview(reviewId, response));
    }
}
