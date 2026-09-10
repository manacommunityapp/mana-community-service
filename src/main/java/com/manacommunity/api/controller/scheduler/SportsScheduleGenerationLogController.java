package com.manacommunity.api.controller.scheduler;

import com.manacommunity.api.dto.scheduler.SportsScheduleGenerationLogResponse;
import com.manacommunity.api.user.model.AppUser;
import com.manacommunity.api.user.security.UserPrincipal;
import com.manacommunity.api.user.service.LoggedInUserService;
import com.manacommunity.api.service.scheduler.SportsScheduleGenerationLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST endpoints for reading schedule generation audit logs.
 * Logs are append-only — no create/update/delete from the API.
 */
@RestController
@RequestMapping("/api/tournament/logs")
@RequiredArgsConstructor
public class SportsScheduleGenerationLogController {

    private final SportsScheduleGenerationLogService logService;
    private final LoggedInUserService          loggedInUserService;

    /** GET /api/tournament/logs/config/{configId} — logs for a specific tournament config. */
    @GetMapping("/config/{configId}")
    public ResponseEntity<List<SportsScheduleGenerationLogResponse>> getLogsByConfig(
            @PathVariable Long configId) {
        return ResponseEntity.ok(logService.getLogsByConfig(configId));
    }

    /** GET /api/tournament/logs/event/{eventId} — logs for a specific sports event. */
    @GetMapping("/event/{eventId}")
    public ResponseEntity<List<SportsScheduleGenerationLogResponse>> getLogsByEvent(
            @PathVariable Long eventId) {
        return ResponseEntity.ok(logService.getLogsByEvent(eventId));
    }

    /** GET /api/tournament/logs — all logs for the logged-in user's community (paginated). */
    @GetMapping
    public ResponseEntity<Page<SportsScheduleGenerationLogResponse>> getAllLogs(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        AppUser user = loggedInUserService.resolve(principal);
        Long communityId = user.getCommunity() != null ? user.getCommunity().getId() : null;
        int safeSize = Math.min(Math.max(size, 1), 100);
        return ResponseEntity.ok(logService.getLogsByCommunity(communityId, Math.max(page, 0), safeSize));
    }
}
