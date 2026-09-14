package com.manacommunity.api.repository;

import com.manacommunity.api.model.SportsPlayerRanking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface SportsPlayerRankingRepository extends JpaRepository<SportsPlayerRanking, Long> {

    Optional<SportsPlayerRanking> findByUserIdAndSportIdAndCommunityIdAndSeason(
            Long userId, Long sportId, Long communityId, String season);

    @Query("SELECT r FROM SportsPlayerRanking r " +
           "WHERE r.sport.id = :sportId AND r.community.id = :communityId AND r.season = :season " +
           "ORDER BY CASE WHEN r.rank IS NULL THEN 1 ELSE 0 END, r.rank ASC, r.rating DESC NULLS LAST")
    List<SportsPlayerRanking> findBySportAndCommunityAndSeason(
            @Param("sportId") Long sportId,
            @Param("communityId") Long communityId,
            @Param("season") String season);

    @Query("SELECT r FROM SportsPlayerRanking r " +
           "WHERE r.user.id = :userId AND r.community.id = :communityId AND r.season = :season")
    List<SportsPlayerRanking> findByUserAndCommunityAndSeason(
            @Param("userId") Long userId,
            @Param("communityId") Long communityId,
            @Param("season") String season);

    Optional<SportsPlayerRanking> findByUserIdAndSportIdAndCommunityId(
            Long userId, Long sportId, Long communityId);
}
