package com.manacommunity.api.hrm.controller;

import com.manacommunity.api.hrm.entity.HrReportConfig;
import com.manacommunity.api.hrm.service.HrEmployeeService;
import com.manacommunity.api.hrm.service.HrReportService;
import com.manacommunity.api.user.model.AppUser;
import com.manacommunity.api.user.security.UserPrincipal;
import com.manacommunity.api.user.service.LoggedInUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/hr/reports")
@RequiredArgsConstructor
public class HrReportController {

    private final HrReportService reportService;
    private final HrEmployeeService employeeService;
    private final LoggedInUserService loggedInUserService;

    @GetMapping("/organizations/{orgId}/configs")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<HrReportConfig>> getReportConfigs(
            @PathVariable Long orgId,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        employeeService.resolveAndVerifyMembership(orgId, user.getId());
        return ResponseEntity.ok(reportService.getReportConfigs(orgId));
    }

    @PostMapping("/organizations/{orgId}/configs")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<HrReportConfig> saveReportConfig(
            @PathVariable Long orgId,
            @RequestBody Map<String, String> body,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        employeeService.resolveAndVerifyMembership(orgId, user.getId());
        var emp = employeeService.findByOrgAndManaUser(orgId, user.getId());
        return ResponseEntity.ok(reportService.saveReportConfig(
                orgId,
                body.get("name"),
                body.get("reportType"),
                body.get("filtersJson"),
                body.get("columnsJson"),
                emp.getId()));
    }

    @DeleteMapping("/organizations/{orgId}/configs/{configId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> deleteReportConfig(
            @PathVariable Long orgId,
            @PathVariable Long configId,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        employeeService.resolveAndVerifyMembership(orgId, user.getId());
        reportService.deleteReportConfig(orgId, configId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/organizations/{orgId}/export")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<byte[]> exportReport(
            @PathVariable Long orgId,
            @RequestParam(defaultValue = "HEADCOUNT") String reportType,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        employeeService.resolveAndVerifyMembership(orgId, user.getId());
        byte[] csv = reportService.generateCsvReport(orgId, reportType);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        headers.setContentDispositionFormData("attachment", reportType.toLowerCase() + "_report.csv");
        return ResponseEntity.ok().headers(headers).body(csv);
    }
}
