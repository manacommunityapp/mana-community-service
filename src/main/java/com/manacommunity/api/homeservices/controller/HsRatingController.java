package com.manacommunity.api.homeservices.controller;

import com.manacommunity.api.homeservices.entity.HsBookingRating;
import com.manacommunity.api.homeservices.service.HsRatingService;
import com.manacommunity.api.user.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/home-services/ratings")
@RequiredArgsConstructor
public class HsRatingController {

    private final HsRatingService ratingService;

    @PostMapping
    public ResponseEntity<HsBookingRating> rate(@RequestBody HsBookingRating rating,
            @AuthenticationPrincipal UserPrincipal p) {
        rating.setUserId(p.getUserId());
        return ResponseEntity.ok(ratingService.rate(rating));
    }

    @GetMapping("/worker/{workerId}")
    public ResponseEntity<List<HsBookingRating>> workerRatings(@PathVariable Long workerId) {
        return ResponseEntity.ok(ratingService.workerRatings(workerId));
    }
}
