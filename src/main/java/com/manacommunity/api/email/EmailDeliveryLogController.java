package com.manacommunity.api.email;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * Admin-only endpoint for querying the email delivery log.
 *
 * <p>All operations require {@code ROLE_ADMIN} or {@code ROLE_SUPER_ADMIN}.
 * Results are paginated, newest first, and filterable by status, template type,
 * community, recipient email fragment, and date range.</p>
 *
 * <p>Example queries:</p>
 * <ul>
 *   <li>All failures today: {@code GET /api/admin/email/delivery-log?status=FAILED&from=2026-08-05}</li>
 *   <li>OTP emails to a user: {@code GET /api/admin/email/delivery-log?templateType=OTP&recipient=ravi@gmail.com}</li>
 *   <li>Community-scoped view: {@code GET /api/admin/email/delivery-log?communityId=3}</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/admin/email")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN', 'COMMUNITY_ADMIN')")
public class EmailDeliveryLogController {

    private final EmailDeliveryLogRepository repo;

    /**
     * Paginated, filterable delivery log.
     *
     * @param status       filter by status: SENT | FAILED | SKIPPED (optional)
     * @param templateType filter by template type, e.g. OTP, MATCH_REMINDER (optional)
     * @param communityId  restrict to a specific community (optional)
     * @param recipient    partial email address match, case-insensitive (optional)
     * @param from         include only emails sent on or after this date (optional, yyyy-MM-dd)
     * @param to           include only emails sent on or before this date (optional, yyyy-MM-dd)
     * @param page         0-based page index (default 0)
     * @param size         page size, max 200 (default 50)
     */
    @GetMapping("/delivery-log")
    public ResponseEntity<Page<EmailDeliveryLogDto>> getDeliveryLog(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String templateType,
            @RequestParam(required = false) Long communityId,
            @RequestParam(required = false) String recipient,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {

        int safeSize = Math.min(size, 200);
        PageRequest pageable = PageRequest.of(page, safeSize, Sort.by(Sort.Direction.DESC, "sentAt"));

        LocalDateTime fromDt = from != null ? from.atStartOfDay() : null;
        LocalDateTime toDt   = to   != null ? to.atTime(23, 59, 59) : null;

        Page<EmailDeliveryLogDto> result = repo
                .findFiltered(
                        status       != null ? status.toUpperCase() : null,
                        templateType,
                        communityId,
                        recipient,
                        fromDt,
                        toDt,
                        pageable)
                .map(EmailDeliveryLogDto::from);

        return ResponseEntity.ok(result);
    }

    /**
     * Summary counts for admin dashboard widgets.
     * Returns total sent/failed/skipped counts for a community in the last N days.
     *
     * @param communityId  the community to summarise
     * @param days         look-back window in days (default 7)
     */
    @GetMapping("/delivery-log/summary")
    public ResponseEntity<Map<String, Object>> getSummary(
            @RequestParam Long communityId,
            @RequestParam(defaultValue = "7") int days) {

        LocalDateTime since = LocalDateTime.now().minusDays(days);

        long sent    = repo.countByCommunityIdAndStatusAndSentAtAfter(communityId, EmailDeliveryLog.STATUS_SENT,    since);
        long failed  = repo.countByCommunityIdAndStatusAndSentAtAfter(communityId, EmailDeliveryLog.STATUS_FAILED,  since);
        long skipped = repo.countByCommunityIdAndStatusAndSentAtAfter(communityId, EmailDeliveryLog.STATUS_SKIPPED, since);

        return ResponseEntity.ok(Map.of(
                "communityId", communityId,
                "periodDays",  days,
                "sent",        sent,
                "failed",      failed,
                "skipped",     skipped,
                "total",       sent + failed + skipped
        ));
    }

    /**
     * Get a single delivery log entry by id (for admin drill-down).
     */
    @GetMapping("/delivery-log/{id}")
    public ResponseEntity<EmailDeliveryLogDto> getOne(@PathVariable Long id) {
        return repo.findById(id)
                .map(EmailDeliveryLogDto::from)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
