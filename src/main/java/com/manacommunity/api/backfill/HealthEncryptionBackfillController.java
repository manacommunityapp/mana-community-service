package com.manacommunity.api.backfill;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.manacommunity.api.user.security.UserPrincipal;

import java.util.Map;

/**
 * SUPER_ADMIN-only trigger for the one-shot health-data encryption backfill.
 *
 * <pre>
 *   POST /api/admin/backfill/health-encryption                 → dry run (default), writes nothing
 *   POST /api/admin/backfill/health-encryption?dryRun=false    → performs the backfill
 * </pre>
 *
 * Two independent gates guard a mutating run, because the blast radius is medical records:
 * the caller must hold {@code ROLE_SUPER_ADMIN}, and the deployment must have opted in with
 * {@code mana.backfill.health-encryption.enabled=true}. A dry run only needs the role, so an
 * operator can always see what the job <em>would</em> do before anything is enabled.
 *
 * <p>The run is synchronous and can take a while on a large table; progress is logged per
 * batch, and because each batch commits in its own transaction a client timeout does not
 * undo the work already done — the job is re-runnable.
 *
 * <p>{@code /api/admin/**} falls through to {@code anyRequest().authenticated()} in
 * SecurityConfig, so the role check here is the authorization boundary.
 */
@Slf4j
@RestController
@RequestMapping("/api/admin/backfill")
@RequiredArgsConstructor
public class HealthEncryptionBackfillController {

    private final HealthEncryptionBackfillJob job;

    @PostMapping("/health-encryption")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<Object> runHealthEncryptionBackfill(
            @RequestParam(defaultValue = "true") boolean dryRun,
            @AuthenticationPrincipal UserPrincipal principal) {

        if (!dryRun && !job.isEnabled()) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of(
                    "error", "Backfill is not enabled on this deployment",
                    "remedy", "Set " + HealthEncryptionBackfillJob.ENABLED_PROPERTY + "=true and retry, "
                            + "or call with dryRun=true to see what it would do."));
        }

        log.warn("Health-encryption backfill requested by userId={} (dryRun={})",
                principal != null ? principal.getId() : null, dryRun);

        return ResponseEntity.ok(job.run(dryRun));
    }
}
