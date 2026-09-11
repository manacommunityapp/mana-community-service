package com.manacommunity.api.repository.scheduler;

import com.manacommunity.api.model.scheduler.MatchStatus;
import com.manacommunity.api.model.scheduler.SportsTournamentMatch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface SportsTournamentMatchRepository extends JpaRepository<SportsTournamentMatch, Long> {

    /**
     * Published matches that start within the reminder window ({@code now..cutoff}) and
     * have not yet had a reminder sent. Drives the per-player "match starts soon" email.
     */
    @Query("""
           SELECT m FROM SportsTournamentMatch m
           WHERE m.status = com.manacommunity.api.model.scheduler.MatchStatus.PUBLISHED
             AND m.reminderSent = false
             AND m.scheduledAt IS NOT NULL
             AND m.scheduledAt > :now
             AND m.scheduledAt <= :cutoff
           ORDER BY m.scheduledAt
           """)
    List<SportsTournamentMatch> findDueForReminder(@Param("now") LocalDateTime now,
                                             @Param("cutoff") LocalDateTime cutoff);

    List<SportsTournamentMatch> findByConfigIdOrderByScheduledAt(Long configId);

    List<SportsTournamentMatch> findByConfigId(Long configId);

    @Query("SELECT m FROM SportsTournamentMatch m WHERE m.config.id=:cid AND m.roundNumber=:rn ORDER BY m.matchNumber")
    List<SportsTournamentMatch> findByConfigIdAndRoundNumberOrderByMatchNumber(
        @Param("cid") Long configId, @Param("rn") int roundNumber);

    @Query("SELECT MAX(m.swissRoundNumber) FROM SportsTournamentMatch m WHERE m.config.id=:cid")
    Optional<Integer> findMaxSwissRound(@Param("cid") Long configId);

    @Query("SELECT m FROM SportsTournamentMatch m WHERE m.config.id=:cid ORDER BY m.swissRoundNumber,m.matchNumber")
    List<SportsTournamentMatch> findByConfigIdOrderBySwissRoundNumber(@Param("cid") Long configId);

    @Query("SELECT m FROM SportsTournamentMatch m WHERE m.config.id=:cid AND m.status='SCHEDULED' ORDER BY m.scheduledAt")
    List<SportsTournamentMatch> findUpcoming(@Param("cid") Long configId);

    @Transactional
    @Modifying(clearAutomatically = true)
    @Query("DELETE FROM SportsTournamentMatch m WHERE m.config.id = :configId")
    void deleteByConfigId(@Param("configId") Long configId);

    long countByConfigId(Long configId);

    @Transactional
    @Modifying(clearAutomatically = true)
    @Query("UPDATE SportsTournamentMatch m SET m.status = :status WHERE m.config.id = :configId")
    int updateStatusByConfigId(@Param("configId") Long configId, @Param("status") MatchStatus status);

    @Query("SELECT COUNT(m) FROM SportsTournamentMatch m WHERE m.config.community.id = :communityId")
    long countByCommunityId(@Param("communityId") Long communityId);

    @Query("SELECT COUNT(m) FROM SportsTournamentMatch m WHERE m.config.community.id = :communityId AND m.status = :status")
    long countByCommunityIdAndStatus(@Param("communityId") Long communityId, @Param("status") MatchStatus status);

    long countByStatus(MatchStatus status);
}
