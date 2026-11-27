package com.manacommunity.api.homeservice.controller;

import com.manacommunity.api.homeservice.dto.HomeServiceReportRequest;
import com.manacommunity.api.homeservice.model.entity.HomeServiceReportEntity;
import com.manacommunity.api.homeservice.service.HomeServiceReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import com.manacommunity.api.user.security.UserPrincipal;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

@RestController("homeServiceReportController")
@RequestMapping("/api/v1/home-services/reports")
@RequiredArgsConstructor
public class HomeServiceReportController {
    private final HomeServiceReportService reportService;

    @PostMapping
    public ResponseEntity<HomeServiceReportEntity> createReport(@RequestBody HomeServiceReportRequest req) {
        return ResponseEntity.ok(reportService.createReport(req));
    }

    @GetMapping
    public ResponseEntity<List<HomeServiceReportEntity>> getReports(@AuthenticationPrincipal UserPrincipal principal) {
        String communityId = String.valueOf(principal.getCommunityId());
        return ResponseEntity.ok(reportService.getCommunityReports(communityId));
    }
}
