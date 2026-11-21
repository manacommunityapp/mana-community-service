package com.manacommunity.api.sportsops.service;

import com.manacommunity.api.sportsops.entity.PlayerRating;
import com.manacommunity.api.sportsops.repository.PlayerRatingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PlayerRatingService {
    private final PlayerRatingRepository repo;

    public List<PlayerRating> listForUser(Long communityId, Long userId) {
        return repo.findByCommunityIdAndRatedUserId(communityId, userId);
    }

    public List<PlayerRating> listAll(Long communityId) {
        return repo.findByCommunityId(communityId);
    }

    public Double averageRating(Long communityId, Long userId) {
        return repo.avgRating(communityId, userId);
    }

    @Transactional
    public PlayerRating rate(PlayerRating rating) {
        if (rating.getRating() < 1 || rating.getRating() > 5) {
            throw new IllegalArgumentException("Rating must be 1-5");
        }
        return repo.save(rating);
    }
}
