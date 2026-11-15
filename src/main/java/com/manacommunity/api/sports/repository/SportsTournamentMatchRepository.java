package com.manacommunity.api.sports.repository;

import com.manacommunity.api.sports.model.MatchStatus;
import com.manacommunity.api.sports.scheduler.SportsTournamentMatch;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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
           WHERE m.status = com.manacommunity.api.sports.model.MatchStatus.PUBLISHED
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

    @Query("SELECT m FROM SportsTournamentMatch m WHERE m.config.community.id = :communityId ORDER BY m.scheduledAt DESC")
    Page<SportsTournamentMatch> findPageByCommunityId(@Param("communityId") Long communityId, Pageable pageable);

    @Query("SELECT m FROM SportsTournamentMatch m WHERE m.config.community.id = :communityId AND m.status = :status ORDER BY m.scheduledAt DESC")
    Page<SportsTournamentMatch> findPageByCommunityIdAndStatus(@Param("communityId") Long communityId, @Param("status") MatchStatus status, Pageable pageable);

    @Query("SELECT m FROM SportsTournamentMatch m WHERE m.config.community.id = :communityId AND m.status = com.manacommunity.api.sports.model.MatchStatus.LIVE ORDER BY m.scheduledAt")
    List<SportsTournamentMatch> findLiveByCommunityId(@Param("communityId") Long communityId);

    /** Live matches, plus matches completed since {@code since}, for a community (feed candidates; flag filtered in service). */
    @Query("""
           SELECT m FROM SportsTournamentMatch m
           WHERE (m.community.id = :communityId
                  OR m.config.community.id = :communityId
                  OR (m.config.event IS NOT NULL AND m.config.event.community.id = :communityId)
                  OR (m.config.event IS NOT NULL AND m.config.event.tournament IS NOT NULL AND m.config.event.tournament.community.id = :communityId))
             AND (m.status = com.manacommunity.api.sports.model.MatchStatus.LIVE
                  OR m.status = com.manacommunity.api.sports.model.MatchStatus.IN_PROGRESS
                  OR (m.status = com.manacommunity.api.sports.model.MatchStatus.COMPLETED
                      AND ((m.completedAt IS NOT NULL AND m.completedAt >= :since)
                           OR (m.completedAt IS NULL AND m.scheduledAt >= :since))))
           ORDER BY m.scheduledAt DESC
           """)
    List<SportsTournamentMatch> findFeedCandidatesByCommunityId(@Param("communityId") Long communityId,
                                                                @Param("since") LocalDateTime since);

    /** Live matches, plus matches completed since {@code since}, across all communities (for super-admin feed). */
    @Query("""
           SELECT m FROM SportsTournamentMatch m
           WHERE (m.status = com.manacommunity.api.sports.model.MatchStatus.LIVE
                  OR m.status = com.manacommunity.api.sports.model.MatchStatus.IN_PROGRESS
                  OR (m.status = com.manacommunity.api.sports.model.MatchStatus.COMPLETED
                      AND ((m.completedAt IS NOT NULL AND m.completedAt >= :since)
                           OR (m.completedAt IS NULL AND m.scheduledAt >= :since))))
           ORDER BY m.scheduledAt DESC
           """)
    List<SportsTournamentMatch> findFeedCandidatesAll(@Param("since") LocalDateTime since);

    /** Upcoming scheduled matches for a community, ordered earliest scheduled first. */
    @Query("""
           SELECT m FROM SportsTournamentMatch m
           WHERE (m.community.id = :communityId
                  OR m.config.community.id = :communityId
                  OR (m.config.event IS NOT NULL AND m.config.event.community.id = :communityId)
                  OR (m.config.event IS NOT NULL AND m.config.event.tournament IS NOT NULL AND m.config.event.tournament.community.id = :communityId))
             AND (m.status = com.manacommunity.api.sports.model.MatchStatus.SCHEDULED
                  OR m.status = com.manacommunity.api.sports.model.MatchStatus.PUBLISHED)
             AND m.scheduledAt >= :since
           ORDER BY m.scheduledAt ASC
           """)
    List<SportsTournamentMatch> findUpcomingCandidatesByCommunityId(@Param("communityId") Long communityId,
                                                                   @Param("since") LocalDateTime since,
                                                                   Pageable pageable);

    /** Upcoming scheduled matches across all communities, ordered earliest scheduled first. */
    @Query("""
           SELECT m FROM SportsTournamentMatch m
           WHERE (m.status = com.manacommunity.api.sports.model.MatchStatus.SCHEDULED
                  OR m.status = com.manacommunity.api.sports.model.MatchStatus.PUBLISHED)
             AND m.scheduledAt >= :since
           ORDER BY m.scheduledAt ASC
           """)
    List<SportsTournamentMatch> findUpcomingCandidatesAll(@Param("since") LocalDateTime since, Pageable pageable);

    /** Recently completed matches for a community, ordered newest completed first. */
    @Query("""
           SELECT m FROM SportsTournamentMatch m
           WHERE (m.community.id = :communityId
                  OR m.config.community.id = :communityId
                  OR (m.config.event IS NOT NULL AND m.config.event.community.id = :communityId)
                  OR (m.config.event IS NOT NULL AND m.config.event.tournament IS NOT NULL AND m.config.event.tournament.community.id = :communityId))
             AND m.status = com.manacommunity.api.sports.model.MatchStatus.COMPLETED
           ORDER BY COALESCE(m.completedAt, m.scheduledAt) DESC
           """)
    List<SportsTournamentMatch> findRecentCompletedByCommunityId(@Param("communityId") Long communityId, Pageable pageable);

    /** Recently completed matches across all communities, ordered newest completed first. */
    @Query("""
           SELECT m FROM SportsTournamentMatch m
           WHERE m.status = com.manacommunity.api.sports.model.MatchStatus.COMPLETED
           ORDER BY COALESCE(m.completedAt, m.scheduledAt) DESC
           """)
    List<SportsTournamentMatch> findRecentCompletedAll(Pageable pageable);

    /** Recently completed matches that have not had a feed result post published yet. */
    @Query("""
           SELECT m FROM SportsTournamentMatch m
           WHERE m.status = com.manacommunity.api.sports.model.MatchStatus.COMPLETED
             AND m.feedResultPostId IS NULL
             AND m.scheduledAt >= :since
           ORDER BY m.scheduledAt
           """)
    List<SportsTournamentMatch> findCompletedWithoutFeedPost(@Param("since") LocalDateTime since);

    @Transactional
    @Modifying(clearAutomatically = true)
    @Query("DELETE FROM SportsTournamentMatch m WHERE m.config.id = :configId")
    void deleteByConfigId(@Param("configId") Long configId);

    long countByConfigId(Long configId);

    List<SportsTournamentMatch> findByStageId(Long stageId);

    @Query("SELECT m FROM SportsTournamentMatch m WHERE m.config.id = :configId AND m.stageId IS NULL AND m.round = :round ORDER BY m.scheduledAt")
    List<SportsTournamentMatch> findUnassignedByConfigIdAndRound(@Param("configId") Long configId, @Param("round") com.manacommunity.api.sports.model.MatchRound round);

    @Query("SELECT m FROM SportsTournamentMatch m WHERE m.config.id = :configId AND m.stageId IS NULL AND m.round IN :rounds ORDER BY m.roundNumber, m.matchNumber")
    List<SportsTournamentMatch> findUnassignedByConfigIdAndRounds(@Param("configId") Long configId, @Param("rounds") java.util.Collection<com.manacommunity.api.sports.model.MatchRound> rounds);

    @Query("SELECT m FROM SportsTournamentMatch m WHERE m.config.id = :configId AND m.stageId IS NULL ORDER BY m.scheduledAt")
    List<SportsTournamentMatch> findUnassignedByConfigId(@Param("configId") Long configId);

    @Transactional
    @Modifying(clearAutomatically = false)
    @Query("UPDATE SportsTournamentMatch m SET m.status = :status WHERE m.config.id = :configId")
    int updateStatusByConfigId(@Param("configId") Long configId, @Param("status") MatchStatus status);

    @Query("SELECT COUNT(m) FROM SportsTournamentMatch m WHERE m.config.community.id = :communityId")
    long countByCommunityId(@Param("communityId") Long communityId);

    @Query("SELECT COUNT(m) FROM SportsTournamentMatch m WHERE m.config.community.id = :communityId AND m.status = :status")
    long countByCommunityIdAndStatus(@Param("communityId") Long communityId, @Param("status") MatchStatus status);

    long countByStatus(MatchStatus status);
}
