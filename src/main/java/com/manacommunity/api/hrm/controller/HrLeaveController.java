package com.manacommunity.api.hrm.controller;

import com.manacommunity.api.hrm.dto.*;
import com.manacommunity.api.hrm.entity.HrEmployee;
import com.manacommunity.api.hrm.entity.HrLeaveType;
import com.manacommunity.api.hrm.entity.HrOrganization;
import com.manacommunity.api.hrm.enums.LeaveApprovalStatus;
import com.manacommunity.api.hrm.service.HrEmployeeService;
import com.manacommunity.api.hrm.service.HrLeaveService;
import com.manacommunity.api.hrm.service.HrOrganizationService;
import com.manacommunity.api.user.model.AppUser;
import com.manacommunity.api.user.security.UserPrincipal;
import com.manacommunity.api.user.service.LoggedInUserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/hr/leaves")
@RequiredArgsConstructor
public class HrLeaveController {

    private final HrLeaveService leaveService;
    private final HrEmployeeService employeeService;
    private final HrOrganizationService organizationService;
    private final LoggedInUserService loggedInUserService;

    @PostMapping("/apply")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<LeaveRequestResponse> applyLeave(
            @RequestParam Long orgId,
            @Valid @RequestBody ApplyLeaveRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        HrEmployee employee = employeeService.findByOrgAndManaUser(orgId, user.getId());
        return ResponseEntity.ok(leaveService.applyLeave(employee, request));
    }

    @GetMapping("/balance")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<LeaveBalanceResponse>> getLeaveBalance(
            @RequestParam Long orgId,
            @RequestParam(required = false) Integer year,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        HrEmployee employee = employeeService.findByOrgAndManaUser(orgId, user.getId());
        int effectiveYear = year != null ? year : LocalDate.now().getYear();
        return ResponseEntity.ok(leaveService.getLeaveBalance(employee.getId(), effectiveYear));
    }

    @GetMapping("/my-requests")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Page<LeaveRequestResponse>> getMyRequests(
            @RequestParam Long orgId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        HrEmployee employee = employeeService.findByOrgAndManaUser(orgId, user.getId());
        return ResponseEntity.ok(leaveService.getMyLeaveRequests(employee.getId(), PageRequest.of(page, size)));
    }

    @GetMapping("/organizations/{orgId}/requests")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Page<LeaveRequestResponse>> getOrgRequests(
            @PathVariable Long orgId,
            @RequestParam(required = false) LeaveApprovalStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        employeeService.resolveAndVerifyMembership(orgId, user.getId());
        LeaveApprovalStatus effectiveStatus = status != null ? status : LeaveApprovalStatus.PENDING;
        return ResponseEntity.ok(leaveService.getOrgLeaveRequests(orgId, effectiveStatus, PageRequest.of(page, size)));
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<LeaveRequestResponse> approveLeave(
            @PathVariable Long id,
            @RequestParam Long orgId,
            @RequestBody(required = false) LeaveActionRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        if (request == null) request = new LeaveActionRequest();
        return ResponseEntity.ok(leaveService.approveLeave(id, orgId, user.getId(), request));
    }

    @PostMapping("/{id}/reject")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<LeaveRequestResponse> rejectLeave(
            @PathVariable Long id,
            @RequestParam Long orgId,
            @RequestBody(required = false) LeaveActionRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        if (request == null) request = new LeaveActionRequest();
        return ResponseEntity.ok(leaveService.rejectLeave(id, orgId, user.getId(), request));
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<LeaveRequestResponse> cancelLeave(
            @PathVariable Long id,
            @RequestParam Long orgId,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        HrEmployee employee = employeeService.findByOrgAndManaUser(orgId, user.getId());
        return ResponseEntity.ok(leaveService.cancelLeave(id, employee.getId()));
    }

    @PostMapping("/organizations/{orgId}/leave-types")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<HrLeaveType> createLeaveType(
            @PathVariable Long orgId,
            @Valid @RequestBody CreateLeaveTypeRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        Long communityId = user.getCommunity() != null ? user.getCommunity().getId() : null;
        HrOrganization org = organizationService.findByIdAndCommunityId(orgId, communityId);
        return ResponseEntity.ok(leaveService.createLeaveType(org, request));
    }

    @GetMapping("/organizations/{orgId}/leave-types")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<HrLeaveType>> listLeaveTypes(@PathVariable Long orgId) {
        return ResponseEntity.ok(leaveService.listLeaveTypes(orgId));
    }
}
