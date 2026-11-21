package com.manacommunity.api.sportsops.controller;

import com.manacommunity.api.sportsops.entity.PlayerRating;
import com.manacommunity.api.sportsops.service.PlayerRatingService;
import com.manacommunity.api.user.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/sports-ops/ratings")
@RequiredArgsConstructor
public class PlayerRatingController {
    private final PlayerRatingService service;

    @GetMapping
    public List<PlayerRating> listAll(@AuthenticationPrincipal UserPrincipal principal) {
        return service.listAll(principal.getCommunityId());
    }

    @GetMapping("/user/{userId}")
    public List<PlayerRating> listForUser(@AuthenticationPrincipal UserPrincipal principal,
                                           @PathVariable Long userId) {
        return service.listForUser(principal.getCommunityId(), userId);
    }

    @GetMapping("/user/{userId}/average")
    public Map<String, Object> average(@AuthenticationPrincipal UserPrincipal principal,
                                        @PathVariable Long userId) {
        return Map.of("userId", userId, "average", service.averageRating(principal.getCommunityId(), userId));
    }

    @PostMapping
    public PlayerRating rate(@AuthenticationPrincipal UserPrincipal principal,
                              @RequestBody PlayerRating rating) {
        rating.setCommunityId(principal.getCommunityId());
        rating.setRatedBy(principal.getUserId());
        return service.rate(rating);
    }
}
