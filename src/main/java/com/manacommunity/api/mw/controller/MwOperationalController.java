package com.manacommunity.api.mw.controller;

import com.manacommunity.api.hrm.service.HrEmployeeService;
import com.manacommunity.api.mw.dto.CompleteAssignmentRequest;
import com.manacommunity.api.mw.dto.CreateOperationalAssignmentRequest;
import com.manacommunity.api.mw.service.MwOperationalAssignmentService;
import com.manacommunity.api.user.model.AppUser;
import com.manacommunity.api.user.security.UserPrincipal;
import com.manacommunity.api.user.service.LoggedInUserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/mw/operational")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class MwOperationalController {

    private final MwOperationalAssignmentService assignmentService;
    private final LoggedInUserService loggedInUserService;
    private final HrEmployeeService employeeService;

    @PostMapping("/orgs/{orgId}/assignments")
    public ResponseEntity<?> createAssignment(
            @PathVariable Long orgId,
            @Valid @RequestBody CreateOperationalAssignmentRequest req,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        employeeService.resolveAndVerifyMembership(orgId, user.getId());
        return ResponseEntity.ok(assignmentService.createAssignment(orgId, req));
    }

    @PutMapping("/orgs/{orgId}/assignments/{assignmentId}/start")
    public ResponseEntity<?> startAssignment(
            @PathVariable Long orgId,
            @PathVariable Long assignmentId,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        employeeService.resolveAndVerifyMembership(orgId, user.getId());
        return ResponseEntity.ok(assignmentService.startAssignment(orgId, assignmentId));
    }

    @PutMapping("/orgs/{orgId}/assignments/{assignmentId}/complete")
    public ResponseEntity<?> completeAssignment(
            @PathVariable Long orgId,
            @PathVariable Long assignmentId,
            @RequestBody CompleteAssignmentRequest req,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        employeeService.resolveAndVerifyMembership(orgId, user.getId());
        return ResponseEntity.ok(assignmentService.completeAssignment(orgId, assignmentId, req));
    }

    @GetMapping("/orgs/{orgId}/assignments")
    public ResponseEntity<?> getAssignments(
            @PathVariable Long orgId,
            @RequestParam(required = false) String module,
            @RequestParam(required = false) String status,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        employeeService.resolveAndVerifyMembership(orgId, user.getId());
        return ResponseEntity.ok(assignmentService.getAssignments(orgId, module, status));
    }

    @GetMapping("/orgs/{orgId}/employees/{employeeId}/assignments")
    public ResponseEntity<?> getEmployeeAssignments(
            @PathVariable Long orgId,
            @PathVariable Long employeeId,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        employeeService.resolveAndVerifyMembership(orgId, user.getId());
        return ResponseEntity.ok(assignmentService.getEmployeeAssignments(orgId, employeeId));
    }
}
