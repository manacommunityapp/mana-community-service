package com.manacommunity.resident.dashboard;

import com.manacommunity.api.platform.web.ApiResponse;
import com.manacommunity.api.user.security.UserPrincipal;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Resident Dashboard")
@RestController
@RequestMapping("/api/resident/dashboard")
@RequiredArgsConstructor
public class ResidentDashboardController {

    private final ResidentDashboardService dashboardService;

    @GetMapping("/snapshot")
    public ResponseEntity<ApiResponse<ResidentDashboardSnapshot>> getSnapshot(
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(
                ApiResponse.ok(dashboardService.buildSnapshot(principal.getId())));
    }
}
