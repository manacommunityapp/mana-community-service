package com.manacommunity.api.finance.compliance.controller;

import com.manacommunity.api.finance.compliance.dto.GenerateReportRequest;
import com.manacommunity.api.finance.compliance.dto.UpsertConfigRequest;
import com.manacommunity.api.finance.compliance.entity.ComplianceReportConfig;
import com.manacommunity.api.finance.compliance.entity.GeneratedReport;
import com.manacommunity.api.finance.compliance.repository.ComplianceReportConfigRepository;
import com.manacommunity.api.finance.compliance.repository.GeneratedReportRepository;
import com.manacommunity.api.finance.compliance.service.ComplianceReportService;
import com.manacommunity.api.platform.web.ApiResponse;
import com.manacommunity.api.user.model.AppUser;
import com.manacommunity.api.user.security.UserPrincipal;
import com.manacommunity.api.user.service.LoggedInUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/compliance")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('FACILITY_MANAGER','ADMIN','SUPER_ADMIN')")
public class ComplianceController {

    private final ComplianceReportService reportService;
    private final GeneratedReportRepository reportRepo;
    private final ComplianceReportConfigRepository configRepo;
    private final LoggedInUserService loggedInUserService;

    @GetMapping("/reports")
    public ResponseEntity<ApiResponse<List<GeneratedReport>>> listReports(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String period) {
        Long communityId = principal.getCommunityId();
        List<GeneratedReport> reports;
        if (type != null && period != null) {
            reports = reportRepo.findByCommunityIdAndReportTypeAndReportPeriodOrderByGeneratedAtDesc(
                    communityId, type, period);
        } else if (type != null) {
            reports = reportRepo.findByCommunityIdAndReportTypeOrderByGeneratedAtDesc(communityId, type);
        } else {
            reports = reportRepo.findByCommunityIdOrderByGeneratedAtDesc(communityId);
        }
        return ResponseEntity.ok(ApiResponse.ok(reports));
    }

    @PostMapping("/reports/generate")
    public ResponseEntity<ApiResponse<GeneratedReport>> generateReport(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestBody GenerateReportRequest req) {
        AppUser user = loggedInUserService.resolve(principal);
        GeneratedReport report = reportService.generateOnDemand(req, user.getId());
        return ResponseEntity.ok(ApiResponse.ok(report, "Report generated"));
    }

    @GetMapping("/reports/{id}/download")
    public ResponseEntity<ApiResponse<GeneratedReport>> getReport(@PathVariable Long id) {
        return reportRepo.findById(id)
                .map(r -> ResponseEntity.ok(ApiResponse.ok(r)))
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/configs")
    public ResponseEntity<ApiResponse<List<ComplianceReportConfig>>> listConfigs(@AuthenticationPrincipal UserPrincipal principal
) {
        Long communityId = principal.getCommunityId();
        return ResponseEntity.ok(ApiResponse.ok(
                configRepo.findByCommunityIdAndIsActiveTrue(communityId)));
    }

    @PostMapping("/configs")
    public ResponseEntity<ApiResponse<ComplianceReportConfig>> upsertConfig(
            @RequestBody UpsertConfigRequest req) {
        return ResponseEntity.ok(ApiResponse.ok(reportService.upsertConfig(req), "Config saved"));
    }
}
