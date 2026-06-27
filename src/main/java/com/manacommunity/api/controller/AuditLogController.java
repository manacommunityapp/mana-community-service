package com.manacommunity.api.controller;

import com.manacommunity.api.model.AuditLog;
import com.manacommunity.api.repository.AuditLogRepository;
import com.manacommunity.api.security.AuditAction;
import com.manacommunity.api.security.AuditModule;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Read-only access to the durable audit trail for the admin monitoring dashboard.
 * SUPER_ADMIN only — the audit log can contain sensitive actor / entity references.
 */
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AuditLogController {

    private final AuditLogRepository auditLogRepository;

    public record AuditLogDto(
            Long id, Long userId, String action, String module,
            String entityName, String entityId, String oldValue, String newValue,
            String ipAddress, String correlationId, LocalDateTime createdAt) {

        static AuditLogDto from(AuditLog a) {
            return new AuditLogDto(a.getId(), a.getUserId(), a.getAction(), a.getModule(),
                    a.getEntityName(), a.getEntityId(), a.getOldValue(), a.getNewValue(),
                    a.getIpAddress(), a.getCorrelationId(), a.getCreatedAt());
        }
    }

    public record AuditPageResponse(
            List<AuditLogDto> content, int page, int size, long totalElements, int totalPages) {}

    public record AuditStatsResponse(
            long eventsToday, long usersCreatedToday, long auctionEventsToday,
            long permissionChangesToday, long bidsToday) {}

    /** GET /api/admin/audit-logs — paged, newest first, optional module/action/userId filters. */
    @GetMapping("/audit-logs")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<AuditPageResponse> getAuditLogs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
            @RequestParam(required = false) String module,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) Long userId) {

        int safeSize = Math.min(Math.max(size, 1), 200);
        Page<AuditLog> result = auditLogRepository.search(
                blankToNull(module), blankToNull(action), userId, PageRequest.of(Math.max(page, 0), safeSize));

        List<AuditLogDto> content = result.getContent().stream().map(AuditLogDto::from).toList();
        return ResponseEntity.ok(new AuditPageResponse(
                content, result.getNumber(), result.getSize(), result.getTotalElements(), result.getTotalPages()));
    }

    /** GET /api/admin/audit-logs/stats — counts for "today" for the dashboard widgets. */
    @GetMapping("/audit-logs/stats")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<AuditStatsResponse> getAuditStats() {
        LocalDateTime since = LocalDate.now().atStartOfDay();
        return ResponseEntity.ok(new AuditStatsResponse(
                auditLogRepository.countByCreatedAtAfter(since),
                auditLogRepository.countByActionAndCreatedAtAfter(AuditAction.USER_CREATED.name(), since),
                auditLogRepository.countByModuleAndCreatedAtAfter(AuditModule.AUCTION.name(), since),
                auditLogRepository.countByActionAndCreatedAtAfter(AuditAction.PERMISSION_CHANGED.name(), since),
                auditLogRepository.countByActionAndCreatedAtAfter(AuditAction.BID_PLACED.name(), since)));
    }

    private static String blankToNull(String s) {
        return (s == null || s.isBlank()) ? null : s;
    }
}
