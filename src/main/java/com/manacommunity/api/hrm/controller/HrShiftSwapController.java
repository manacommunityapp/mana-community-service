package com.manacommunity.api.hrm.controller;

import com.manacommunity.api.hrm.entity.HrEmployee;
import com.manacommunity.api.hrm.entity.HrShiftSwapRequest;
import com.manacommunity.api.hrm.service.HrEmployeeService;
import com.manacommunity.api.hrm.service.HrShiftSwapService;
import com.manacommunity.api.user.model.AppUser;
import com.manacommunity.api.user.security.UserPrincipal;
import com.manacommunity.api.user.service.LoggedInUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/hr/shifts/swap")
@RequiredArgsConstructor
public class HrShiftSwapController {

    private final HrShiftSwapService swapService;
    private final HrEmployeeService employeeService;
    private final LoggedInUserService loggedInUserService;

    @PostMapping("/request")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<HrShiftSwapRequest> requestSwap(
            @RequestParam Long orgId,
            @RequestParam Long targetEmployeeId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate myShiftDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate targetShiftDate,
            @RequestParam(required = false) String reason,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        HrEmployee requester = employeeService.findByOrgAndManaUser(orgId, user.getId());
        return ResponseEntity.ok(swapService.requestSwap(
                orgId, requester.getId(), targetEmployeeId, myShiftDate, targetShiftDate, reason));
    }

    @PostMapping("/{swapId}/colleague-respond")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<HrShiftSwapRequest> colleagueRespond(
            @PathVariable Long swapId,
            @RequestParam Long orgId,
            @RequestParam boolean accepted,
            @RequestParam(required = false) String rejectionReason,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        employeeService.resolveAndVerifyMembership(orgId, user.getId());
        return ResponseEntity.ok(swapService.colleagueRespond(swapId, accepted, rejectionReason));
    }

    @PostMapping("/{swapId}/manager-approve")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<HrShiftSwapRequest> managerApprove(
            @PathVariable Long swapId,
            @RequestParam Long orgId,
            @RequestParam boolean approved,
            @RequestParam(required = false) String reason,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        employeeService.resolveAndVerifyMembership(orgId, user.getId());
        return ResponseEntity.ok(swapService.managerRespond(swapId, approved, user.getId(), reason));
    }

    @GetMapping("/my")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<HrShiftSwapRequest>> getMySwapRequests(
            @RequestParam Long orgId,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        HrEmployee employee = employeeService.findByOrgAndManaUser(orgId, user.getId());
        return ResponseEntity.ok(swapService.getMySwapRequests(employee.getId()));
    }

    @GetMapping("/incoming")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<HrShiftSwapRequest>> getIncomingSwapRequests(
            @RequestParam Long orgId,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        HrEmployee employee = employeeService.findByOrgAndManaUser(orgId, user.getId());
        return ResponseEntity.ok(swapService.getIncomingSwapRequests(employee.getId()));
    }

    @GetMapping("/organizations/{orgId}/pending")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<HrShiftSwapRequest>> getPendingManagerApprovals(
            @PathVariable Long orgId,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        employeeService.resolveAndVerifyMembership(orgId, user.getId());
        return ResponseEntity.ok(swapService.getPendingManagerApprovals(orgId));
    }
}
