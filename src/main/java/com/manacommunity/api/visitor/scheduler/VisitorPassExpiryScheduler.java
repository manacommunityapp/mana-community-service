package com.manacommunity.api.visitor.scheduler;

import com.manacommunity.api.visitor.entity.VisitorPass;
import com.manacommunity.api.visitor.repository.VisitorPassRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Periodically moves visitor passes that have passed their expected arrival window
 * into {@code EXPIRED} status.
 *
 * <p>Two sweeps per run:
 * <ol>
 *   <li>Passes with {@code expectedAt} set and now past {@code expectedAt + graceHours}.
 *   <li>PENDING passes with no {@code expectedAt} that are older than {@code staleHours}
 *       (walk-ins or pre-approvals where the host never responded).
 * </ol>
 *
 * <p>Both sweeps use a single bulk UPDATE so the job remains O(1) on DB round-trips
 * regardless of how many passes match.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class VisitorPassExpiryScheduler {

    private final VisitorPassRepository visitorPassRepository;

    /**
     * Hours after {@code expectedAt} before a pass is expired.
     * Default: 2 hours — covers late arrivals while keeping the gate log clean.
     */
    @Value("${visitor.pass.expiry.grace-hours:2}")
    private int graceHours;

    /**
     * Hours after {@code createdAt} before a PENDING pass with no {@code expectedAt}
     * is considered stale and expired. Default: 24 hours.
     */
    @Value("${visitor.pass.expiry.stale-hours:24}")
    private int staleHours;

    private static final List<VisitorPass.PassStatus> EXPIRABLE_STATUSES = List.of(
            VisitorPass.PassStatus.PENDING,
            VisitorPass.PassStatus.APPROVED,
            VisitorPass.PassStatus.EXPECTED
    );

    /** Runs every 30 minutes. */
    @Scheduled(cron = "0 */30 * * * *")
    @Transactional
    public void expireOverduePasses() {
        LocalDateTime now = LocalDateTime.now();

        // Sweep 1: passes that have an expectedAt and the grace window has closed
        LocalDateTime graceCutoff = now.minusHours(graceHours);
        int expiredByExpectedAt = visitorPassRepository.bulkExpireByExpectedAt(
                EXPIRABLE_STATUSES, graceCutoff, now);

        // Sweep 2: PENDING passes with no expectedAt that are older than staleHours
        LocalDateTime staleCutoff = now.minusHours(staleHours);
        int expiredStale = visitorPassRepository.bulkExpireStaleNoExpectedAt(staleCutoff, now);

        int total = expiredByExpectedAt + expiredStale;
        if (total > 0) {
            log.info("VisitorPassExpiryScheduler: expired {} passes ({} by expectedAt, {} stale no-date)",
                    total, expiredByExpectedAt, expiredStale);
        } else {
            log.debug("VisitorPassExpiryScheduler: no passes to expire");
        }
    }
}
