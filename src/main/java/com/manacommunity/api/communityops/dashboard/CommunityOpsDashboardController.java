package com.manacommunity.api.communityops.dashboard;

import com.manacommunity.api.repository.CommunityRepository;
import com.manacommunity.api.user.security.UserPrincipal;
import com.manacommunity.api.platform.web.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Community Ops Dashboard")
@RestController
@RequestMapping("/api/community/ops")
@RequiredArgsConstructor
@Slf4j
public class CommunityOpsDashboardController {

    private final CommunityOpsDashboardService dashboardService;
    private final SimpMessagingTemplate messagingTemplate;
    private final CommunityRepository communityRepository;

    @GetMapping("/snapshot")
    public ResponseEntity<ApiResponse<CommunityOpsSnapshot>> getSnapshot(
            @RequestParam Long communityId,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.ok(dashboardService.buildSnapshot(communityId)));
    }

    @Scheduled(fixedRate = 30_000)
    public void pushSnapshotsToAll() {
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
