package com.manacommunity.api.sportsops.repository;

import com.manacommunity.api.sportsops.entity.PlayerRatingSummary;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface PlayerRatingSummaryRepository extends JpaRepository<PlayerRatingSummary, Long> {
    Optional<PlayerRatingSummary> findByUserIdAndSport(Long userId, String sport);
    List<PlayerRatingSummary> findByCommunityIdAndSportOrderByAvgRatingDesc(Long communityId, String sport);
}
