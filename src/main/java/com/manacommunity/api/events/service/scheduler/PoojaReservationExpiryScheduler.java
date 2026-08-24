package com.manacommunity.api.events.service.scheduler;

import com.manacommunity.api.events.repository.EventPoojaSlotReservationRepository;
import com.manacommunity.api.security.AuditAction;
import com.manacommunity.api.security.AuditModule;
import com.manacommunity.api.security.AuditService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Runs every 60 seconds and bulk-expires RESERVED rows whose hold has timed out.
 * This is a safety net — the per-schedule expiry inside {@code reserve()} handles
 * real-time correctness; this cleans up stale rows for long-idle slots.
 */
@Component
public class PoojaReservationExpiryScheduler {

    private static final Logger log = LoggerFactory.getLogger(PoojaReservationExpiryScheduler.class);

    private final EventPoojaSlotReservationRepository reservationRepo;
    private final AuditService auditService;

    @Value("${pooja.reservation.purge-days:30}")
    private int purgeOlderThanDays;

    public PoojaReservationExpiryScheduler(EventPoojaSlotReservationRepository reservationRepo,
                                            AuditService auditService) {
        this.reservationRepo = reservationRepo;
        this.auditService = auditService;
    }

    @Scheduled(fixedDelayString = "${pooja.reservation.expiry-check-ms:60000}")
    @Transactional
    public void expireStaleReservations() {
        LocalDateTime now = LocalDateTime.now();

        // Expire RESERVED rows whose TTL has elapsed
        int expired = reservationRepo.expireAllStale(now);
        if (expired > 0) {
            log.info("Expired {} stale pooja slot reservations", expired);
            auditService.record(AuditAction.POOJA_SLOT_RESERVATION_EXPIRED, AuditModule.EVENTS,
                    "EventPoojaSlotReservation", "batch", null, "count=" + expired);
        }

        // #23: Purge old EXPIRED / CANCELLED rows to prevent unbounded table growth
        LocalDateTime purgeBeforeDate = now.minusDays(purgeOlderThanDays);
        int purged = reservationRepo.deleteExpiredOrCancelledBefore(purgeBeforeDate);
        if (purged > 0) {
            log.info("Purged {} old EXPIRED/CANCELLED pooja reservations older than {} days", purged, purgeOlderThanDays);
        }
    }
}
