package com.manacommunity.api.analytics.service;

import com.manacommunity.api.analytics.model.CommunityInsight;
import com.manacommunity.api.analytics.model.CommunityMetricsSnapshot;
import com.manacommunity.api.analytics.repository.CommunityInsightRepository;
import com.manacommunity.api.analytics.repository.CommunityMetricsSnapshotRepository;
import com.manacommunity.api.helpdesk.entity.Ticket;
import com.manacommunity.api.helpdesk.repository.TicketRepository;
import jakarta.persistence.EntityManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@Transactional
public class CommunityInsightsService {

    private static final Logger log = LoggerFactory.getLogger(CommunityInsightsService.class);

    private final CommunityMetricsSnapshotRepository snapshotRepo;
    private final CommunityInsightRepository insightRepo;
    private final TicketRepository ticketRepo;
    private final EntityManager em;

    public CommunityInsightsService(
            CommunityMetricsSnapshotRepository snapshotRepo,
            CommunityInsightRepository insightRepo,
            TicketRepository ticketRepo,
            EntityManager em) {
        this.snapshotRepo = snapshotRepo;
        this.insightRepo = insightRepo;
        this.ticketRepo = ticketRepo;
        this.em = em;
    }

    public CommunityMetricsSnapshot snapshotMetrics(Long communityId) {
        LocalDate today = LocalDate.now();
        CommunityMetricsSnapshot s = snapshotRepo
                .findByCommunityIdAndSnapshotDate(communityId, today)
                .orElse(new CommunityMetricsSnapshot());
        s.setCommunityId(communityId);
        s.setSnapshotDate(today);

        // Open complaints from helpdesk
        List<Ticket> openTickets = ticketRepo.findByCommunityAndStatuses(
                communityId, List.of(Ticket.TicketStatus.OPEN, Ticket.TicketStatus.IN_PROGRESS));
        s.setOpenComplaints(openTickets.size());

        // Average resolution hours for recently resolved tickets (last 30 days)
        try {
            Number avgHours = (Number) em.createNativeQuery(
                    "SELECT AVG(EXTRACT(EPOCH FROM (resolved_at - created_at))/3600) " +
                    "FROM helpdesk_tickets WHERE community_id = :cid " +
                    "AND status IN ('RESOLVED','CLOSED') AND resolved_at >= NOW() - INTERVAL '30 days'")
                    .setParameter("cid", communityId)
                    .getSingleResult();
            if (avgHours != null) s.setAvgResolutionHours(BigDecimal.valueOf(avgHours.doubleValue()).setScale(1, RoundingMode.HALF_UP));
        } catch (Exception e) {
            log.debug("Could not query resolution hours: {}", e.getMessage());
        }

        // Financial: collection rate from invoices
        try {
            Object[] fin = (Object[]) em.createNativeQuery(
                    "SELECT SUM(CASE WHEN status = 'UNPAID' THEN amount ELSE 0 END), " +
                    "       COUNT(*), SUM(CASE WHEN status = 'PAID' THEN 1 ELSE 0 END) " +
                    "FROM invoices WHERE community_id = :cid AND due_date >= NOW() - INTERVAL '90 days'")
                    .setParameter("cid", communityId)
                    .getSingleResult();
            if (fin[0] != null) s.setTotalDuesOutstanding(((Number)fin[0]).doubleValue() > 0 ? BigDecimal.valueOf(((Number)fin[0]).doubleValue()) : BigDecimal.ZERO);
            if (fin[1] != null && fin[2] != null) {
                long total = ((Number)fin[1]).longValue();
                long paid  = ((Number)fin[2]).longValue();
                if (total > 0) s.setCollectionRate(BigDecimal.valueOf(paid * 100.0 / total).setScale(1, RoundingMode.HALF_UP));
            }
        } catch (Exception e) {
            log.debug("Could not query financial metrics: {}", e.getMessage());
        }

        // Gate entries today + unrecognised (HOLD = vehicle not in registry)
        try {
            Number entries = (Number) em.createNativeQuery(
                    "SELECT COUNT(*) FROM anpr_gate_events WHERE community_id = :cid AND created_at::date = CURRENT_DATE")
                    .setParameter("cid", communityId)
                    .getSingleResult();
            if (entries != null) s.setGateEntriesToday(entries.intValue());

            Number unrecog = (Number) em.createNativeQuery(
                    "SELECT COUNT(*) FROM anpr_gate_events WHERE community_id = :cid AND barrier_action = 'HOLD' AND created_at::date = CURRENT_DATE")
                    .setParameter("cid", communityId)
                    .getSingleResult();
            if (unrecog != null) s.setUnrecognisedEntries(unrecog.intValue());
        } catch (Exception e) {
            log.debug("Could not query gate events: {}", e.getMessage());
        }

        // Events and votes this month
        try {
            Number evtCount = (Number) em.createNativeQuery(
                    "SELECT COUNT(*) FROM cultural_events WHERE community_id = :cid AND DATE_TRUNC('month', created_at) = DATE_TRUNC('month', NOW())")
                    .setParameter("cid", communityId)
                    .getSingleResult();
            if (evtCount != null) s.setEventsThisMonth(evtCount.intValue());
        } catch (Exception e) {
            log.debug("Could not query events: {}", e.getMessage());
        }

        return snapshotRepo.save(s);
    }

    public List<CommunityInsight> generateInsights(Long communityId) {
        // Expire old non-dismissed insights
        insightRepo.findByCommunityIdAndDismissedFalseOrderBySeverityAscGeneratedAtDesc(communityId)
                .forEach(i -> { i.setDismissed(true); insightRepo.save(i); });

        List<CommunityInsight> insights = new ArrayList<>();
        List<CommunityMetricsSnapshot> last30 = snapshotRepo.findRecentByCommunityId(
                communityId, LocalDate.now().minusDays(30));

        if (last30.isEmpty()) {
            return insights;
        }

        CommunityMetricsSnapshot latest = last30.get(last30.size() - 1);

        // MAINTENANCE_ALERT: avg resolution > 48h with >5 open tickets
        if (latest.getAvgResolutionHours() != null && latest.getOpenComplaints() != null
                && latest.getAvgResolutionHours().compareTo(BigDecimal.valueOf(48)) > 0
                && latest.getOpenComplaints() > 5) {
            insights.add(buildInsight(communityId, "MAINTENANCE_ALERT", "ALERT",
                    "Maintenance response times are slipping",
                    "Average resolution time is " + latest.getAvgResolutionHours().setScale(0, RoundingMode.HALF_UP) + "h with " + latest.getOpenComplaints() + " open complaints.",
                    "Prioritise OPEN tickets and assign to available staff.",
                    latest.getAvgResolutionHours(), "hours", "last_30_days"));
        }

        // PAYMENT_TREND: collection rate dropped >10% vs prior 30 days
        if (last30.size() >= 2 && latest.getCollectionRate() != null) {
            BigDecimal prior = last30.get(0).getCollectionRate();
            if (prior != null) {
                BigDecimal drop = prior.subtract(latest.getCollectionRate());
                if (drop.compareTo(BigDecimal.TEN) > 0) {
                    insights.add(buildInsight(communityId, "PAYMENT_TREND", "WARNING",
                            "Maintenance collection rate is dropping",
                            "Collection rate fell from " + prior.setScale(0, RoundingMode.HALF_UP) + "% to " + latest.getCollectionRate().setScale(0, RoundingMode.HALF_UP) + "%.",
                            "Send payment reminders to residents with outstanding dues.",
                            drop, "percentage_points", "last_30_days"));
                }
            }
        }

        // POSITIVE_TREND: collection rate > 95%
        if (latest.getCollectionRate() != null && latest.getCollectionRate().compareTo(BigDecimal.valueOf(95)) >= 0) {
            insights.add(buildInsight(communityId, "POSITIVE_TREND", "INFO",
                    "Excellent collection rate — well done!",
                    "Maintenance collection is at " + latest.getCollectionRate().setScale(0, RoundingMode.HALF_UP) + "%, above the 95% benchmark.",
                    null, latest.getCollectionRate(), "%", "last_30_days"));
        }

        // SECURITY_ANOMALY: unrecognised entries > 5 today
        if (latest.getUnrecognisedEntries() != null && latest.getUnrecognisedEntries() > 5) {
            insights.add(buildInsight(communityId, "SECURITY_ANOMALY", "ALERT",
                    "Unusual number of unrecognised vehicle entries",
                    latest.getUnrecognisedEntries() + " vehicles were held at the gate today due to unrecognised plates.",
                    "Review ANPR log and ensure guard follows up on unrecognised vehicles.",
                    BigDecimal.valueOf(latest.getUnrecognisedEntries()), "entries", "today"));
        }

        // PARTICIPATION_DROP: active residents < 20%
        if (latest.getActiveResidentsPct() != null && latest.getActiveResidentsPct().compareTo(BigDecimal.valueOf(20)) < 0) {
            insights.add(buildInsight(communityId, "PARTICIPATION_DROP", "INFO",
                    "Low resident app engagement",
                    "Only " + latest.getActiveResidentsPct().setScale(0, RoundingMode.HALF_UP) + "% of residents are active on Mana.",
                    "Consider sending a community newsletter or running an event to re-engage residents.",
                    latest.getActiveResidentsPct(), "%", "last_30_days"));
        }

        insights.forEach(insightRepo::save);
        return insights;
    }

    public void dismissInsight(Long insightId, Long dismissedBy) {
        insightRepo.findById(insightId).ifPresent(i -> {
            i.setDismissed(true);
            i.setDismissedBy(dismissedBy);
            insightRepo.save(i);
        });
    }

    public List<CommunityInsight> getActiveInsights(Long communityId) {
        return insightRepo.findByCommunityIdAndDismissedFalseOrderBySeverityAscGeneratedAtDesc(communityId);
    }

    public List<CommunityMetricsSnapshot> getMetricsTrend(Long communityId, int days) {
        return snapshotRepo.findRecentByCommunityId(communityId, LocalDate.now().minusDays(days));
    }

    @Scheduled(cron = "0 0 1 * * *")
    public void dailyInsightRun() {
        log.info("Running daily community insights snapshot");
        try {
            List<Long> communityIds = em.createNativeQuery(
                    "SELECT id FROM communities WHERE is_active = true")
                    .getResultList()
                    .stream()
                    .map(r -> ((Number) r).longValue())
                    .toList();
            for (Long cid : communityIds) {
                try {
                    snapshotMetrics(cid);
                    generateInsights(cid);
                } catch (Exception e) {
                    log.error("Failed insights run for community {}: {}", cid, e.getMessage());
                }
            }
        } catch (Exception e) {
            log.error("Daily insight run failed: {}", e.getMessage());
        }
    }

    private CommunityInsight buildInsight(Long communityId, String type, String severity,
            String title, String description, String recommendation,
            BigDecimal value, String unit, String period) {
        CommunityInsight i = new CommunityInsight();
        i.setCommunityId(communityId);
        i.setInsightType(type);
        i.setSeverity(severity);
        i.setTitle(title);
        i.setDescription(description);
        i.setRecommendation(recommendation);
        i.setMetricValue(value);
        i.setMetricUnit(unit);
        i.setReferencePeriod(period);
        i.setExpiresAt(OffsetDateTime.now().plusDays(7));
        return i;
    }
}
