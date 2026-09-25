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

@Slf4j
@Component
@RequiredArgsConstructor
public class MatchNotificationScheduler {

    @PersistenceContext
    private final EntityManager em;

    private final ApplicationEventPublisher events;

    @Scheduled(fixedDelay = 5 * 60 * 1000)
    @Transactional
    public void notifyUpcomingMatches() {
        LocalDateTime windowStart = LocalDateTime.now().plusMinutes(25);
        LocalDateTime windowEnd   = LocalDateTime.now().plusMinutes(35);

        @SuppressWarnings("unchecked")
        List<Object[]> rows = em.createQuery(
            """
            SELECT m.id, m.teamA.teamName, m.teamB.teamName, m.config.sport.name,
                   m.venue.name, m.scheduledAt, m.community.id
            FROM SportsTournamentMatch m
            WHERE m.scheduledAt BETWEEN :windowStart AND :windowEnd
              AND m.status = com.manacommunity.api.model.scheduler.MatchStatus.SCHEDULED
              AND m.reminderSent = false
            """)
            .setParameter("windowStart", windowStart)
            .setParameter("windowEnd",   windowEnd)
            .getResultList();

        for (Object[] row : rows) {
            Long   matchId     = ((Number) row[0]).longValue();
            String teamAName   = row[1] != null ? (String) row[1] : "Team A";
            String teamBName   = row[2] != null ? (String) row[2] : "Team B";
            String sport       = row[3] != null ? (String) row[3] : "Sports";
            String venue       = row[4] != null ? (String) row[4] : "Community ground";
            LocalDateTime scheduledAt = (LocalDateTime) row[5];
            Long   communityId = row[6] != null ? ((Number) row[6]).longValue() : null;

            String matchTitle  = teamAName + " vs " + teamBName;
            String scheduledStr = scheduledAt.format(DateTimeFormatter.ofPattern("h:mm a"));

            @SuppressWarnings("unchecked")
            List<Long> playerIds = em.createQuery(
                """
                SELECT DISTINCT p.user.id
                FROM SportsAuctionPlayer p
                WHERE p.user IS NOT NULL
                  AND (p.assignedTeam.id IN (SELECT m.teamA.id FROM SportsTournamentMatch m WHERE m.id = :matchId)
                       OR p.assignedTeam.id IN (SELECT m.teamB.id FROM SportsTournamentMatch m WHERE m.id = :matchId))
                """)
                .setParameter("matchId", matchId)
                .getResultList();

            events.publishEvent(new MatchStartingSoonEvent(
                this, matchId, matchTitle, sport, venue, scheduledStr,
                playerIds, communityId
            ));

            em.createQuery(
                "UPDATE SportsTournamentMatch m SET m.reminderSent = true, m.reminderSentAt = :now WHERE m.id = :id")
                .setParameter("now", LocalDateTime.now())
                .setParameter("id",  matchId)
                .executeUpdate();

            log.info("[MatchScheduler] 'Starting soon' notification fired: matchId={} title='{}' players={}",
                     matchId, matchTitle, playerIds.size());
        }
    }
}
