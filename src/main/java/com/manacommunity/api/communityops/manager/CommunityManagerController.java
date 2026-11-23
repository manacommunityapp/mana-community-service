package com.manacommunity.api.communityops.manager;

import com.manacommunity.api.platform.web.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Community Manager Dashboard")
@RestController
@RequestMapping("/api/community/manager")
@PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','COMMUNITY_ADMIN','COMMUNITY_MGMT_ADMIN')")
@RequiredArgsConstructor
public class CommunityManagerController {

    private final CommunityManagerService managerService;

    @GetMapping("/snapshot")
    public ResponseEntity<ApiResponse<CommunityManagerSnapshot>> getSnapshot(
            @RequestParam Long communityId) {
        return ResponseEntity.ok(ApiResponse.ok(managerService.getSnapshot(communityId)));
    }

    @PostMapping("/snapshot/refresh")
    public ResponseEntity<ApiResponse<CommunityManagerSnapshot>> refreshSnapshot(
            @RequestParam Long communityId) {
        return ResponseEntity.ok(ApiResponse.ok(managerService.refreshSnapshot(communityId)));
    }
}
