package com.manacommunity.api.hrm.controller;

import com.manacommunity.api.hrm.entity.*;
import com.manacommunity.api.hrm.service.HrEmployeeService;
import com.manacommunity.api.hrm.service.HrPayrollService;
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
@RequestMapping("/api/v1/hr/payroll")
@RequiredArgsConstructor
public class HrPayrollController {

    private final HrPayrollService payrollService;
    private final HrEmployeeService employeeService;
    private final LoggedInUserService loggedInUserService;

    @PostMapping("/organizations/{orgId}/runs")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<HrPayrollRun> createRun(
            @PathVariable Long orgId,
            @RequestBody Map<String, Integer> body,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        employeeService.resolveAndVerifyMembership(orgId, user.getId());
        HrEmployee emp = employeeService.findByOrgAndManaUser(orgId, user.getId());
        return ResponseEntity.ok(payrollService.createRun(orgId, body.get("month"), body.get("year"), emp.getId()));
    }

    @PostMapping("/organizations/{orgId}/runs/{runId}/process")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<HrPayrollRun> processRun(
            @PathVariable Long orgId,
            @PathVariable Long runId,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        employeeService.resolveAndVerifyMembership(orgId, user.getId());
        return ResponseEntity.ok(payrollService.processPayroll(orgId, runId));
    }

    @PostMapping("/organizations/{orgId}/runs/{runId}/approve")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<HrPayrollRun> approveRun(
            @PathVariable Long orgId,
            @PathVariable Long runId,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        employeeService.resolveAndVerifyMembership(orgId, user.getId());
        HrEmployee emp = employeeService.findByOrgAndManaUser(orgId, user.getId());
        return ResponseEntity.ok(payrollService.approvePayroll(orgId, runId, emp.getId()));
    }

    @PostMapping("/organizations/{orgId}/runs/{runId}/publish")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> publishPayslips(
            @PathVariable Long orgId,
            @PathVariable Long runId,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        employeeService.resolveAndVerifyMembership(orgId, user.getId());
        payrollService.publishPayslips(orgId, runId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/organizations/{orgId}/runs")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<HrPayrollRun>> getRuns(
            @PathVariable Long orgId,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        employeeService.resolveAndVerifyMembership(orgId, user.getId());
        return ResponseEntity.ok(payrollService.getRuns(orgId));
    }

    @GetMapping("/organizations/{orgId}/runs/{runId}/payslips")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<HrPayslip>> getRunPayslips(
            @PathVariable Long orgId,
            @PathVariable Long runId,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        employeeService.resolveAndVerifyMembership(orgId, user.getId());
        return ResponseEntity.ok(payrollService.getPayslips(runId));
    }

    @GetMapping("/my-payslips")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<HrPayslip>> getMyPayslips(
            @RequestParam Long orgId,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        HrEmployee emp = employeeService.findByOrgAndManaUser(orgId, user.getId());
        return ResponseEntity.ok(payrollService.getMyPayslips(emp.getId()));
    }

    @PostMapping("/organizations/{orgId}/employees/{employeeId}/salary-structure")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<HrSalaryStructure> setSalaryStructure(
            @PathVariable Long orgId,
            @PathVariable Long employeeId,
            @RequestBody HrSalaryStructure structure,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        employeeService.resolveAndVerifyMembership(orgId, user.getId());
        HrEmployee emp = employeeService.findByOrgAndManaUser(orgId, user.getId());
        return ResponseEntity.ok(payrollService.setSalaryStructure(orgId, employeeId, structure, emp.getId()));
    }

    @GetMapping("/employees/{employeeId}/salary-history")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<HrSalaryStructure>> getSalaryHistory(
            @PathVariable Long employeeId,
            @RequestParam Long orgId,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        employeeService.resolveAndVerifyMembership(orgId, user.getId());
        return ResponseEntity.ok(payrollService.getSalaryHistory(employeeId));
    }

    @GetMapping("/organizations/{orgId}/components")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<HrSalaryComponent>> getComponents(
            @PathVariable Long orgId,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        employeeService.resolveAndVerifyMembership(orgId, user.getId());
        return ResponseEntity.ok(payrollService.getComponents(orgId));
    }

    @PostMapping("/organizations/{orgId}/components")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<HrSalaryComponent> saveComponent(
            @PathVariable Long orgId,
            @RequestBody HrSalaryComponent component,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        employeeService.resolveAndVerifyMembership(orgId, user.getId());
        return ResponseEntity.ok(payrollService.saveComponent(orgId, component));
    }
}
