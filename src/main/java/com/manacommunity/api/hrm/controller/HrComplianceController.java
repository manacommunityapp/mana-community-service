package com.manacommunity.api.hrm.controller;

import com.manacommunity.api.hrm.entity.HrComplianceAlert;
import com.manacommunity.api.hrm.service.HrComplianceService;
import com.manacommunity.api.hrm.service.HrEmployeeService;
import com.manacommunity.api.user.model.AppUser;
import com.manacommunity.api.user.security.UserPrincipal;
import com.manacommunity.api.user.service.LoggedInUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/hr/compliance")
@RequiredArgsConstructor
public class HrComplianceController {

    private final HrComplianceService complianceService;
    private final HrEmployeeService employeeService;
    private final LoggedInUserService loggedInUserService;

    @GetMapping("/organizations/{orgId}/alerts")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<HrComplianceAlert>> getAlerts(
            @PathVariable Long orgId,
            @RequestParam(defaultValue = "false") boolean all,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        employeeService.resolveAndVerifyMembership(orgId, user.getId());
        List<HrComplianceAlert> alerts = all
                ? complianceService.getAllAlerts(orgId)
                : complianceService.getOpenAlerts(orgId);
        return ResponseEntity.ok(alerts);
    }

    @PostMapping("/organizations/{orgId}/alerts/generate")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Object>> generateAlerts(
            @PathVariable Long orgId,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        employeeService.resolveAndVerifyMembership(orgId, user.getId());
        int count = complianceService.generateComplianceAlerts(orgId);
        return ResponseEntity.ok(Map.of("generated", count));
    }

    @PutMapping("/organizations/{orgId}/alerts/{alertId}/acknowledge")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<HrComplianceAlert> acknowledge(
            @PathVariable Long orgId,
            @PathVariable Long alertId,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        employeeService.resolveAndVerifyMembership(orgId, user.getId());
        var emp = employeeService.findByOrgAndManaUser(orgId, user.getId());
        return ResponseEntity.ok(complianceService.acknowledgeAlert(orgId, alertId, emp.getId()));
    }

    @PutMapping("/organizations/{orgId}/alerts/{alertId}/resolve")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<HrComplianceAlert> resolve(
            @PathVariable Long orgId,
            @PathVariable Long alertId,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        employeeService.resolveAndVerifyMembership(orgId, user.getId());
        return ResponseEntity.ok(complianceService.resolveAlert(orgId, alertId));
    }

    @PutMapping("/organizations/{orgId}/alerts/{alertId}/dismiss")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<HrComplianceAlert> dismiss(
            @PathVariable Long orgId,
            @PathVariable Long alertId,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        employeeService.resolveAndVerifyMembership(orgId, user.getId());
        return ResponseEntity.ok(complianceService.dismissAlert(orgId, alertId));
    }
}
