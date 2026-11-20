package com.manacommunity.api.sportsops.repository;

import com.manacommunity.api.sportsops.entity.PlayerRating;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;

public interface PlayerRatingRepository extends JpaRepository<PlayerRating, Long> {
    List<PlayerRating> findByCommunityIdAndRatedUserId(Long communityId, Long ratedUserId);
    List<PlayerRating> findByCommunityId(Long communityId);
    @Query("SELECT AVG(r.rating) FROM PlayerRating r WHERE r.communityId = :communityId AND r.ratedUserId = :userId")
    Double avgRating(Long communityId, Long userId);
}
