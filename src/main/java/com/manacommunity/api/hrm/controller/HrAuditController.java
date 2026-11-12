package com.manacommunity.api.hrm.controller;

import com.manacommunity.api.hrm.entity.HrAuditLog;
import com.manacommunity.api.hrm.service.HrAuditService;
import com.manacommunity.api.hrm.service.HrEmployeeService;
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

@RestController
@RequestMapping("/api/v1/hr/audit")
@RequiredArgsConstructor
public class HrAuditController {

    private final HrAuditService auditService;
    private final HrEmployeeService employeeService;
    private final LoggedInUserService loggedInUserService;

    @GetMapping("/organizations/{orgId}/logs")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Page<HrAuditLog>> getAuditLogs(
            @PathVariable Long orgId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String entityType,
            @RequestParam(required = false) Long entityId,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        employeeService.resolveAndVerifyMembership(orgId, user.getId());
        PageRequest pageRequest = PageRequest.of(page, size);
        if (entityType != null && entityId != null) {
            List<HrAuditLog> logs = auditService.getEntityAuditLogs(orgId, entityType, entityId);
            return ResponseEntity.ok(new org.springframework.data.domain.PageImpl<>(logs, pageRequest, logs.size()));
        } else if (entityType != null) {
            return ResponseEntity.ok(auditService.getAuditLogsByEntityType(orgId, entityType, pageRequest));
        }
        return ResponseEntity.ok(auditService.getAuditLogs(orgId, pageRequest));
    }
}
