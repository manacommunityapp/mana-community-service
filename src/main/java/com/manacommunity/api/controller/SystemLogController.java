package com.manacommunity.api.controller;

import com.manacommunity.api.dto.SystemLogResponse;
import com.manacommunity.api.dto.SystemStatsResponse;
import com.manacommunity.api.user.security.UserPrincipal;
import com.manacommunity.api.service.PermissionCheckService;
import com.manacommunity.api.service.SystemLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import static com.manacommunity.api.constants.PermissionConstants.VIEW_ADMIN;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class SystemLogController {

    private final SystemLogService systemLogService;
    private final PermissionCheckService permissionCheckService;

    /**
     * GET /api/admin/logs?lines=200&level=ERROR&search=keyword&logType=APPLICATION
     * Supported logType values: APPLICATION (default), FRONTEND, DATABASE
     */
    @GetMapping("/logs")
    public ResponseEntity<SystemLogResponse> getLogs(
            @RequestParam(defaultValue = "200") int lines,
            @RequestParam(required = false) String level,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "APPLICATION") String logType,
            @AuthenticationPrincipal UserPrincipal principal) {
        permissionCheckService.requireAnyPermission(principal, VIEW_ADMIN);
        return ResponseEntity.ok(systemLogService.getLogTail(lines, level, search, logType));
    }

    @GetMapping("/log-types")
    public ResponseEntity<java.util.List<String>> getLogTypes(
            @AuthenticationPrincipal UserPrincipal principal) {
        permissionCheckService.requireAnyPermission(principal, VIEW_ADMIN);
        return ResponseEntity.ok(systemLogService.getAvailableLogTypes());
    }

    @GetMapping("/system-stats")
    public ResponseEntity<SystemStatsResponse> getSystemStats(
            @AuthenticationPrincipal UserPrincipal principal) {
        permissionCheckService.requireAnyPermission(principal, VIEW_ADMIN);
        return ResponseEntity.ok(systemLogService.getSystemStats());
    }
}
