package com.manacommunity.api.communityops.dashboard;

import com.manacommunity.api.repository.CommunityRepository;
import com.manacommunity.api.user.security.UserPrincipal;
import com.manacommunity.api.user.model.AppUser;
import com.manacommunity.api.user.service.LoggedInUserService;
import com.manacommunity.api.service.PermissionCheckService;
import com.manacommunity.api.platform.web.ApiResponse;
import com.manacommunity.platform.websocket.WebSocketSessionTracker;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.AccessDeniedException;

import static com.manacommunity.api.constants.PermissionConstants.ROLE_SUPER_ADMIN;
import static com.manacommunity.api.constants.PermissionConstants.VIEW_ADMIN_DASHBOARD;

@Tag(name = "Community Ops Dashboard")
@RestController
@RequestMapping({"/api/community/ops", "/api/community/{communityId}/ops"})
@RequiredArgsConstructor
@Slf4j
public class CommunityOpsDashboardController {

    private final CommunityOpsDashboardService dashboardService;
    private final SimpMessagingTemplate messagingTemplate;
    private final CommunityRepository communityRepository;
    private final LoggedInUserService loggedInUserService;
    private final PermissionCheckService permissionCheckService;

    @Autowired(required = false)
    private WebSocketSessionTracker sessionTracker;

    @GetMapping("/snapshot")
    public ResponseEntity<ApiResponse<CommunityOpsSnapshot>> getSnapshot(
            @PathVariable(name = "communityId", required = false) Long pathCommunityId,
            @RequestParam(required = false) Long communityId,
            @AuthenticationPrincipal UserPrincipal principal) {
        Long targetCommunityId = pathCommunityId != null ? pathCommunityId : communityId;
        if (targetCommunityId == null) {
            throw new IllegalArgumentException("communityId is required");
        }
        permissionCheckService.requireAnyPermission(principal, VIEW_ADMIN_DASHBOARD);
        AppUser caller = loggedInUserService.resolve(principal);
        Long callerCommunityId = caller.getCommunity() != null ? caller.getCommunity().getId() : null;
        if (!caller.hasRole(ROLE_SUPER_ADMIN) && !targetCommunityId.equals(callerCommunityId)) {
            throw new AccessDeniedException("You may only view the operations snapshot for your community");
        }
        return ResponseEntity.ok(ApiResponse.ok(dashboardService.buildSnapshot(targetCommunityId)));
    }

    @Scheduled(fixedRate = 30_000)
    public void pushSnapshotsToAll() {
        if (sessionTracker != null && !sessionTracker.hasActiveSessions()) {
            log.trace("Skipping Ops snapshot push: 0 active WebSocket sessions");
            return;
        }
        communityRepository.findByActiveTrueOrderByNameAsc().forEach(community -> {
            try {
                CommunityOpsSnapshot snapshot = dashboardService.buildSnapshot(community.getId());
                messagingTemplate.convertAndSend(
                        "/topic/community/" + community.getId() + "/ops", snapshot);
            } catch (Exception e) {
                log.warn("Ops snapshot push failed for community {}: {}", community.getId(), e.getMessage());
            }
        });
    }
}
