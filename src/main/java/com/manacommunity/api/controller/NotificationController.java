package com.manacommunity.api.controller;

import com.manacommunity.api.dto.MarkReadRequest;
import com.manacommunity.api.dto.NotificationCountResponse;
import com.manacommunity.api.dto.NotificationResponse;
import com.manacommunity.api.security.UserPrincipal;
import com.manacommunity.api.service.NotificationManagementService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * REST endpoints for the user-facing notification inbox.
 */
@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationManagementService notificationService;

    /** GET /api/notifications — paginated notification feed for the logged-in user. */
    @GetMapping
    public ResponseEntity<Page<NotificationResponse>> getNotifications(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        int safeSize = Math.min(Math.max(size, 1), 100);
        return ResponseEntity.ok(notificationService.getUserNotifications(principal.getId(), Math.max(page, 0), safeSize));
    }

    /** GET /api/notifications/count — unread badge count. */
    @GetMapping("/count")
    public ResponseEntity<NotificationCountResponse> getUnreadCount(
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(notificationService.getUnreadCount(principal.getId()));
    }

    /** PUT /api/notifications/read — mark specific notifications as read. */
    @PutMapping("/read")
    public ResponseEntity<Map<String, Object>> markAsRead(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody MarkReadRequest request) {
        int updated = notificationService.markAsRead(principal.getId(), request.notificationIds());
        return ResponseEntity.ok(Map.of("updated", updated));
    }

    /** PUT /api/notifications/read-all — mark all notifications as read. */
    @PutMapping("/read-all")
    public ResponseEntity<Map<String, Object>> markAllAsRead(
            @AuthenticationPrincipal UserPrincipal principal) {
        int updated = notificationService.markAllAsRead(principal.getId());
        return ResponseEntity.ok(Map.of("updated", updated));
    }

    /** PUT /api/notifications/{id}/dismiss — dismiss a single notification. */
    @PutMapping("/{id}/dismiss")
    public ResponseEntity<Map<String, Object>> dismiss(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id) {
        int updated = notificationService.dismiss(principal.getId(), id);
        return ResponseEntity.ok(Map.of("dismissed", updated));
    }

    /** PUT /api/notifications/dismiss-all — dismiss all notifications. */
    @PutMapping("/dismiss-all")
    public ResponseEntity<Map<String, Object>> dismissAll(
            @AuthenticationPrincipal UserPrincipal principal) {
        int dismissed = notificationService.dismissAll(principal.getId());
        return ResponseEntity.ok(Map.of("dismissed", dismissed));
    }
}
