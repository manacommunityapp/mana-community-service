package com.manacommunity.api.hrm.controller;

import com.manacommunity.api.hrm.dto.HrDashboardResponse;
import com.manacommunity.api.hrm.service.HrDashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/hr/dashboard")
@RequiredArgsConstructor
public class HrDashboardController {

    private final HrDashboardService dashboardService;

    @GetMapping("/organizations/{orgId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<HrDashboardResponse> getDashboard(@PathVariable Long orgId) {
        return ResponseEntity.ok(dashboardService.getDashboard(orgId));
    }
}
