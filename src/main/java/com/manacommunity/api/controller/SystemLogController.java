package com.manacommunity.api.controller;

import com.manacommunity.api.dto.SystemLogResponse;
import com.manacommunity.api.dto.SystemStatsResponse;
import com.manacommunity.api.service.SystemLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class SystemLogController {

    private final SystemLogService systemLogService;

    @GetMapping("/logs")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<SystemLogResponse> getLogs(
            @RequestParam(defaultValue = "200") int lines,
            @RequestParam(required = false) String level,
            @RequestParam(required = false) String search) {
        return ResponseEntity.ok(systemLogService.getLogTail(lines, level, search));
    }

    @GetMapping("/system-stats")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<SystemStatsResponse> getSystemStats() {
        return ResponseEntity.ok(systemLogService.getSystemStats());
    }
}
