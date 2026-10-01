package com.manacommunity.api.sports.repository;

import com.manacommunity.api.sports.model.MatchStatus;
import com.manacommunity.api.sports.scheduler.SportsTournamentMatch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface SportsScheduleAllEventsRepository extends JpaRepository<SportsTournamentMatch, Long> {

    @Query("SELECT COUNT(m) FROM SportsTournamentMatch m WHERE m.config.community.id = :communityId")
    long countByCommunityId(@Param("communityId") Long communityId);

    @Query("SELECT COUNT(m) FROM SportsTournamentMatch m WHERE m.config.community.id = :communityId AND m.status = :status")
    long countByCommunityIdAndStatus(@Param("communityId") Long communityId, @Param("status") MatchStatus status);

    long countByStatus(MatchStatus status);
}
