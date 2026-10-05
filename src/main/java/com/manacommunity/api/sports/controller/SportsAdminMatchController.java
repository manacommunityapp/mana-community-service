package com.manacommunity.api.sports.controller;

import com.manacommunity.api.dto.PagedResponse;
import com.manacommunity.api.exception.ResourceNotFoundException;
import com.manacommunity.api.sports.dto.SportsMatchResponse;
import com.manacommunity.api.sports.model.MatchStatus;
import com.manacommunity.api.sports.repository.SportsTournamentMatchRepository;
import com.manacommunity.api.sports.scheduler.SportsTournamentMatch;
import com.manacommunity.api.sports.scheduler.SportsTournamentSchedulerService;
import com.manacommunity.api.user.security.UserPrincipal;
import com.manacommunity.api.user.service.LoggedInUserService;
import com.manacommunity.api.user.service.LoggedInUserService.ResolvedUser;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

import static com.manacommunity.api.constants.permissions.SportsPermissions.VIEW_SPORTS_MAIN;

@RestController
@RequestMapping("/api/sports")
@RequiredArgsConstructor
public class SportsAdminMatchController {

    private final SportsTournamentMatchRepository matchRepo;
    private final SportsTournamentSchedulerService schedulerService;
    private final LoggedInUserService loggedInUserService;
    private final com.manacommunity.api.service.PermissionCheckService permissionCheckService;

    @GetMapping("/matches")
    public ResponseEntity<PagedResponse<SportsMatchResponse>> getMatches(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
            @AuthenticationPrincipal UserPrincipal principal) {
        permissionCheckService.requireAnyPermission(principal, VIEW_SPORTS_MAIN);
        ResolvedUser ctx = loggedInUserService.resolveContext(principal);
        if (!ctx.superAdmin() && ctx.communityId() == null) {
            return ResponseEntity.ok(PagedResponse.empty());
        }
        int safeSize = Math.min(Math.max(size, 1), 200);
        PageRequest pageable = PageRequest.of(Math.max(page, 0), safeSize,
                Sort.by("scheduledAt").descending());

        Page<SportsTournamentMatch> matchPage;
        if (status != null && !status.isBlank()) {
            MatchStatus matchStatus = MatchStatus.valueOf(status.toUpperCase());
            matchPage = ctx.superAdmin()
                    ? matchRepo.findAll(pageable)
                    : matchRepo.findPageByCommunityIdAndStatus(ctx.communityId(), matchStatus, pageable);
        } else {
            matchPage = ctx.superAdmin()
                    ? matchRepo.findAll(pageable)
                    : matchRepo.findPageByCommunityId(ctx.communityId(), pageable);
        }
        return ResponseEntity.ok(PagedResponse.from(matchPage, schedulerService::toMatchResponse));
    }

    @GetMapping("/matches/live")
    public ResponseEntity<List<SportsMatchResponse>> getLiveMatches(
            @AuthenticationPrincipal UserPrincipal principal) {
        permissionCheckService.requireAnyPermission(principal, VIEW_SPORTS_MAIN);
        ResolvedUser ctx = loggedInUserService.resolveContext(principal);
        if (!ctx.superAdmin() && ctx.communityId() == null) {
            return ResponseEntity.ok(List.of());
        }
        List<SportsTournamentMatch> live = ctx.superAdmin()
                ? matchRepo.findAll().stream()
                        .filter(m -> m.getStatus() == MatchStatus.LIVE).toList()
                : matchRepo.findLiveByCommunityId(ctx.communityId());
        return ResponseEntity.ok(live.stream().map(schedulerService::toMatchResponse).toList());
    }

    @PutMapping("/matches/{matchId}/status")
    public ResponseEntity<SportsMatchResponse> updateMatchStatus(
            @PathVariable Long matchId,
            @RequestBody Map<String, String> body,
            @AuthenticationPrincipal UserPrincipal principal) {
        permissionCheckService.requireAnyPermission(principal, VIEW_SPORTS_MAIN);
        loggedInUserService.resolve(principal);
        SportsTournamentMatch match = matchRepo.findById(matchId)
                .orElseThrow(() -> new ResourceNotFoundException("Match", matchId));
        String statusStr = body.get("status");
        if (statusStr != null && !statusStr.isBlank()) {
            match.setStatus(MatchStatus.valueOf(statusStr.toUpperCase()));
            match = matchRepo.save(match);
        }
        return ResponseEntity.ok(schedulerService.toMatchResponse(match));
    }
}
