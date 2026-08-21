package com.manacommunity.api.email.brevo;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controller exposing Brevo (formerly Sendinblue) Cloud metrics, account status,
 * live transactional event activity, verified senders, and delivery log synchronization.
 */
@RestController
@RequestMapping("/api/admin/email/brevo")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN', 'COMMUNITY_ADMIN')")
public class BrevoEmailController {

    private final BrevoService brevoService;

    /**
     * Get Brevo Account details, plan tier, and daily email relay credits.
     */
    @GetMapping("/account")
    public ResponseEntity<BrevoAccountDto> getAccount() {
        return ResponseEntity.ok(brevoService.getAccountInfo());
    }

    /**
     * Get Brevo Aggregated Transactional Delivery Statistics.
     *
     * @param days lookback window in days (default 7)
     */
    @GetMapping("/stats")
    public ResponseEntity<BrevoStatsDto> getStats(@RequestParam(defaultValue = "7") int days) {
        return ResponseEntity.ok(brevoService.getAggregatedStats(days));
    }

    /**
     * Get Brevo live transactional logs and step events.
     */
    @GetMapping("/logs")
    public ResponseEntity<List<BrevoEmailLogDto>> getLogs(
            @RequestParam(defaultValue = "50") int limit,
            @RequestParam(required = false) String email,
            @RequestParam(required = false) String event) {
        return ResponseEntity.ok(brevoService.getEmailLogs(limit, email, event));
    }

    /**
     * Get verified sender identities and IP status from Brevo.
     */
    @GetMapping("/senders")
    public ResponseEntity<List<BrevoSenderDto>> getSenders() {
        return ResponseEntity.ok(brevoService.getSenders());
    }

    /**
     * Synchronize Brevo event logs into local EmailDeliveryLog repository.
     */
    @PostMapping("/sync")
    public ResponseEntity<BrevoSyncResultDto> syncEvents() {
        return ResponseEntity.ok(brevoService.syncEvents());
    }
}