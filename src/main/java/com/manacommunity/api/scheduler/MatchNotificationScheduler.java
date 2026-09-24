package com.manacommunity.api.scheduler;

import com.manacommunity.api.event.MatchStartingSoonEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Runs every 5 minutes and finds matches starting in 25–35 minutes.
 * Publishes MatchStartingSoonEvent for each match that hasn't been
 * notified yet (startNotificationSentAt IS NULL guard).
 *
 * NOTE: This scheduler requires a `start_notification_sent_at` column
 * on the sports_match table. Add it via V6__sports_match_notification.sql:
 *
 *   ALTER TABLE sports_match ADD COLUMN IF NOT EXISTS
 *       start_notification_sent_at TIMESTAMPTZ;
 *
 * The entity field name is startNotificationSentAt (LocalDateTime).
 * If your Match entity doesn't have this field yet, add it and update
 * the JPQL in this class accordingly.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MatchNotificationScheduler {

    @PersistenceContext
    private final EntityManager em;

    private final ApplicationEventPublisher events;

    /**
     * Every 5 minutes — find matches starting in ~30 minutes that
     * haven't been notified yet.
     */
    @Scheduled(fixedDelay = 5 * 60 * 1000)  // every 5 min
    @Transactional
    public void notifyUpcomingMatches() {
        LocalDateTime windowStart = LocalDateTime.now().plusMinutes(25);
        LocalDateTime windowEnd   = LocalDateTime.now().plusMinutes(35);

        @SuppressWarnings("unchecked")
        List<Object[]> rows = em.createQuery(
            """
            SELECT m.id, m.homeTeam.name, m.awayTeam.name, m.sport,
                   m.venue, m.scheduledAt, m.community.id
            FROM SportMatch m
            WHERE m.scheduledAt BETWEEN :windowStart AND :windowEnd
              AND m.status = 'SCHEDULED'
              AND m.startNotificationSentAt IS NULL
            """)
            .setParameter("windowStart", windowStart)
            .setParameter("windowEnd",   windowEnd)
            .getResultList();

        for (Object[] row : rows) {
            Long   matchId     = ((Number) row[0]).longValue();
            String homeTeam    = (String)  row[1];
            String awayTeam    = (String)  row[2];
            String sport       = (String)  row[3];
            String venue       = row[4] != null ? (String) row[4] : "Community ground";
            LocalDateTime scheduledAt = (LocalDateTime) row[5];
            Long   communityId = ((Number) row[6]).longValue();

            String matchTitle  = homeTeam + " vs " + awayTeam;
            String scheduledStr = scheduledAt.format(DateTimeFormatter.ofPattern("h:mm a"));

            // Fetch participant IDs (all players registered for both teams)
            @SuppressWarnings("unchecked")
            List<Long> playerIds = em.createQuery(
                """
                SELECT DISTINCT tm.user.id
                FROM TeamMember tm
                WHERE tm.team.match.id = :matchId
                """)
                .setParameter("matchId", matchId)
                .getResultList();

            events.publishEvent(new MatchStartingSoonEvent(
                this, matchId, matchTitle, sport, venue, scheduledStr,
                playerIds, communityId
            ));

            // Mark as notified so we don't re-send
            em.createQuery(
                "UPDATE SportMatch m SET m.startNotificationSentAt = :now WHERE m.id = :id")
                .setParameter("now", LocalDateTime.now())
                .setParameter("id",  matchId)
                .executeUpdate();

            log.info("[MatchScheduler] 'Starting soon' notification fired: matchId={} title='{}' players={}",
                     matchId, matchTitle, playerIds.size());
        }
    }
}
