package com.manacommunity.api.hrm.controller;

import com.manacommunity.api.hrm.entity.HrEmployeeRole;
import com.manacommunity.api.hrm.enums.HrmRole;
import com.manacommunity.api.hrm.service.HrEmployeeService;
import com.manacommunity.api.hrm.service.HrRbacService;
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
@RequestMapping("/api/v1/hr/rbac")
@RequiredArgsConstructor
public class HrRbacController {

    private final HrRbacService rbacService;
    private final HrEmployeeService employeeService;
    private final LoggedInUserService loggedInUserService;

    @GetMapping("/organizations/{orgId}/roles")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<HrEmployeeRole>> getAllRoles(
            @PathVariable Long orgId,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        employeeService.resolveAndVerifyMembership(orgId, user.getId());
        return ResponseEntity.ok(rbacService.getAllRoleAssignments(orgId));
    }

    @PostMapping("/organizations/{orgId}/roles")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<HrEmployeeRole> grantRole(
            @PathVariable Long orgId,
            @RequestBody Map<String, Object> body,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        employeeService.resolveAndVerifyMembership(orgId, user.getId());
        Long employeeId = Long.valueOf(body.get("employeeId").toString());
        HrmRole role = HrmRole.valueOf(body.get("role").toString());
        var emp = employeeService.findByOrgAndManaUser(orgId, user.getId());
        return ResponseEntity.ok(rbacService.grantRole(orgId, employeeId, role, emp.getId()));
    }

    @DeleteMapping("/organizations/{orgId}/roles/{roleId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> revokeRole(
            @PathVariable Long orgId,
            @PathVariable Long roleId,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        employeeService.resolveAndVerifyMembership(orgId, user.getId());
        rbacService.revokeRole(orgId, roleId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/organizations/{orgId}/employees/{employeeId}/roles")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<HrEmployeeRole>> getEmployeeRoles(
            @PathVariable Long orgId,
            @PathVariable Long employeeId,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        employeeService.resolveAndVerifyMembership(orgId, user.getId());
        return ResponseEntity.ok(rbacService.getRoles(orgId, employeeId));
    }
}
