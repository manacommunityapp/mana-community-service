package com.manacommunity.api.hrm.controller;

import com.manacommunity.api.hrm.entity.*;
import com.manacommunity.api.hrm.service.HrEmployeeService;
import com.manacommunity.api.hrm.service.HrExpenseService;
import com.manacommunity.api.user.model.AppUser;
import com.manacommunity.api.user.security.UserPrincipal;
import com.manacommunity.api.user.service.LoggedInUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/hr/expenses")
@RequiredArgsConstructor
public class HrExpenseController {

    private final HrExpenseService expenseService;
    private final HrEmployeeService employeeService;
    private final LoggedInUserService loggedInUserService;

    @PostMapping("/organizations/{orgId}/claims")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<HrExpenseClaim> createClaim(
            @PathVariable Long orgId,
            @RequestBody Map<String, Object> body,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        HrEmployee emp = employeeService.findByOrgAndManaUser(orgId, user.getId());
        @SuppressWarnings("unchecked")
        List<HrExpenseItem> items = body.containsKey("items")
            ? (List<HrExpenseItem>) body.get("items") : List.of();
        HrExpenseClaim claim = new HrExpenseClaim();
        claim.setClaimTitle((String) body.get("claimTitle"));
        claim.setClaimType((String) body.get("claimType"));
        claim.setDescription((String) body.get("description"));
        if (body.get("claimDate") != null) {
            claim.setClaimDate(java.time.LocalDate.parse((String) body.get("claimDate")));
        } else {
            claim.setClaimDate(java.time.LocalDate.now());
        }
        return ResponseEntity.ok(expenseService.createClaim(orgId, emp.getId(), claim, items));
    }

    @PostMapping("/organizations/{orgId}/claims/{claimId}/submit")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<HrExpenseClaim> submitClaim(
            @PathVariable Long orgId,
            @PathVariable Long claimId,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        employeeService.resolveAndVerifyMembership(orgId, user.getId());
        return ResponseEntity.ok(expenseService.submitClaim(orgId, claimId));
    }

    @PostMapping("/organizations/{orgId}/claims/{claimId}/approve")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<HrExpenseClaim> approveClaim(
            @PathVariable Long orgId,
            @PathVariable Long claimId,
            @RequestBody(required = false) Map<String, Object> body,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        employeeService.resolveAndVerifyMembership(orgId, user.getId());
        HrEmployee emp = employeeService.findByOrgAndManaUser(orgId, user.getId());
        Double approvedAmount = body != null && body.get("approvedAmount") != null
            ? ((Number) body.get("approvedAmount")).doubleValue() : null;
        return ResponseEntity.ok(expenseService.approveClaim(orgId, claimId, approvedAmount, emp.getId()));
    }

    @PostMapping("/organizations/{orgId}/claims/{claimId}/reject")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<HrExpenseClaim> rejectClaim(
            @PathVariable Long orgId,
            @PathVariable Long claimId,
            @RequestBody Map<String, String> body,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        employeeService.resolveAndVerifyMembership(orgId, user.getId());
        HrEmployee emp = employeeService.findByOrgAndManaUser(orgId, user.getId());
        return ResponseEntity.ok(expenseService.rejectClaim(orgId, claimId, body.get("reason"), emp.getId()));
    }

    @GetMapping("/my-claims")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Page<HrExpenseClaim>> getMyClaims(
            @RequestParam Long orgId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        HrEmployee emp = employeeService.findByOrgAndManaUser(orgId, user.getId());
        return ResponseEntity.ok(expenseService.getMyClaims(emp.getId(), PageRequest.of(page, size)));
    }

    @GetMapping("/organizations/{orgId}/pending")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Page<HrExpenseClaim>> getPendingClaims(
            @PathVariable Long orgId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        employeeService.resolveAndVerifyMembership(orgId, user.getId());
        return ResponseEntity.ok(expenseService.getPendingClaims(orgId, PageRequest.of(page, size)));
    }

    @PostMapping("/organizations/{orgId}/travel")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<HrTravelRequest> createTravelRequest(
            @PathVariable Long orgId,
            @RequestBody HrTravelRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        HrEmployee emp = employeeService.findByOrgAndManaUser(orgId, user.getId());
        return ResponseEntity.ok(expenseService.createTravelRequest(orgId, emp.getId(), request));
    }

    @PostMapping("/organizations/{orgId}/travel/{requestId}/approve")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<HrTravelRequest> approveTravelRequest(
            @PathVariable Long orgId,
            @PathVariable Long requestId,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        employeeService.resolveAndVerifyMembership(orgId, user.getId());
        HrEmployee emp = employeeService.findByOrgAndManaUser(orgId, user.getId());
        return ResponseEntity.ok(expenseService.approveTravelRequest(orgId, requestId, emp.getId()));
    }

    @GetMapping("/my-travel")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Page<HrTravelRequest>> getMyTravel(
            @RequestParam Long orgId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        HrEmployee emp = employeeService.findByOrgAndManaUser(orgId, user.getId());
        return ResponseEntity.ok(expenseService.getMyTravelRequests(emp.getId(), PageRequest.of(page, size)));
    }

    @GetMapping("/organizations/{orgId}/travel/pending")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Page<HrTravelRequest>> getPendingTravel(
            @PathVariable Long orgId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        employeeService.resolveAndVerifyMembership(orgId, user.getId());
        return ResponseEntity.ok(expenseService.getPendingTravelRequests(orgId, PageRequest.of(page, size)));
    }
}
