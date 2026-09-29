package com.manacommunity.api.homeservice.controller;

import com.manacommunity.api.homeservice.dto.HomeServiceReviewRequest;
import com.manacommunity.api.homeservice.model.entity.HomeServiceReviewEntity;
import com.manacommunity.api.homeservice.service.HomeServiceReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController("homeServiceReviewController")
@RequestMapping("/api/v1/home-services/reviews")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class HomeServiceReviewController {
    private final HomeServiceReviewService reviewService;

    @PostMapping
    public ResponseEntity<HomeServiceReviewEntity> submitReview(@RequestBody HomeServiceReviewRequest req) {
        return ResponseEntity.ok(reviewService.submitReview(req));
    }

    @GetMapping("/worker/{workerId}")
    public ResponseEntity<List<HomeServiceReviewEntity>> getReviews(@PathVariable String workerId) {
        return ResponseEntity.ok(reviewService.getReviewsForWorker(workerId));
    }
}
