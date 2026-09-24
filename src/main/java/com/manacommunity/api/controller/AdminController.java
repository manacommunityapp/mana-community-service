package com.manacommunity.api.controller;

import com.manacommunity.api.dto.admin.*;
import com.manacommunity.api.model.AppUser;
import com.manacommunity.api.security.UserPrincipal;
import com.manacommunity.api.service.AdminService;
import com.manacommunity.api.service.LoggedInUserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Admin-only REST controller.
 *
 * All endpoints require the caller to have role ADMIN or SUPER_ADMIN
 * (enforced by @PreAuthorize at the method level so non-admins receive 403).
 *
 * Base path: /api/admin
 */
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final LoggedInUserService loggedInUserService;
    private final AdminService        adminService;

    // ── Helper ────────────────────────────────────────────────────────

    private AppUser resolveAdmin(UserPrincipal principal) {
        return loggedInUserService.resolve(principal);
    }

    // ── Dashboard stats ───────────────────────────────────────────────

    /**
     * GET /api/admin/stats
     * Returns headline counters for the admin dashboard.
     */
    @GetMapping("/stats")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN', 'MODERATOR')")
    public ResponseEntity<AdminStatsResponse> getStats(
            @AuthenticationPrincipal UserPrincipal principal) {

        AppUser admin = resolveAdmin(principal);
        return ResponseEntity.ok(adminService.getStats(admin));
    }

    // ── Member management ─────────────────────────────────────────────

    /**
     * GET /api/admin/members?status=PENDING|ACTIVE|SUSPENDED|ALL&search=&page=0&size=20
     */
    @GetMapping("/members")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN', 'MODERATOR')")
    public ResponseEntity<PagedAdminResponse<AdminMemberResponse>> getMembers(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(defaultValue = "ALL")  String status,
            @RequestParam(required = false)       String search,
            @RequestParam(defaultValue = "0")    int    page,
            @RequestParam(defaultValue = "20")   int    size) {

        AppUser admin = resolveAdmin(principal);
        return ResponseEntity.ok(adminService.getMembers(admin, status, search, page, size));
    }

    /**
     * PUT /api/admin/members/{id}/approve
     * Sets kycStatus=VERIFIED, isActive=true.
     */
    @PutMapping("/members/{id}/approve")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<Void> approveMember(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id) {

        adminService.approveMember(resolveAdmin(principal), id);
        return ResponseEntity.ok().build();
    }

    /**
     * PUT /api/admin/members/{id}/reject
     * Sets kycStatus=REJECTED, isActive=false.
     * Optional body: { "reason": "..." }
     */
    @PutMapping("/members/{id}/reject")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<Void> rejectMember(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id,
            @RequestBody(required = false) MemberActionRequest body) {

        String reason = body != null ? body.getReason() : null;
        adminService.rejectMember(resolveAdmin(principal), id, reason);
        return ResponseEntity.ok().build();
    }

    /**
     * PUT /api/admin/members/{id}/suspend
     * Sets isActive=false. Cannot suspend another ADMIN.
     * Optional body: { "reason": "..." }
     */
    @PutMapping("/members/{id}/suspend")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<Void> suspendMember(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id,
            @RequestBody(required = false) MemberActionRequest body) {

        String reason = body != null ? body.getReason() : null;
        adminService.suspendMember(resolveAdmin(principal), id, reason);
        return ResponseEntity.ok().build();
    }

    /**
     * PUT /api/admin/members/{id}/activate
     * Sets isActive=true, kycStatus=VERIFIED.
     */
    @PutMapping("/members/{id}/activate")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<Void> activateMember(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id) {

        adminService.activateMember(resolveAdmin(principal), id);
        return ResponseEntity.ok().build();
    }

    /**
     * PUT /api/admin/members/{id}/role
     * Body: { "role": "ADMIN" }
     */
    @PutMapping("/members/{id}/role")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<Void> changeMemberRole(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {

        String role = body.getOrDefault("role", "").trim();
        if (role.isBlank()) return ResponseEntity.badRequest().build();
        adminService.changeMemberRole(resolveAdmin(principal), id, role);
        return ResponseEntity.ok().build();
    }

    // ── Content Moderation ────────────────────────────────────────────

    /**
     * GET /api/admin/reports?status=PENDING|RESOLVED|DISMISSED|ALL&page=0&size=20
     */
    @GetMapping("/reports")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN', 'MODERATOR')")
    public ResponseEntity<PagedAdminResponse<ReportResponse>> getReports(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(defaultValue = "PENDING") String status,
            @RequestParam(defaultValue = "0")       int    page,
            @RequestParam(defaultValue = "20")      int    size) {

        AppUser admin = resolveAdmin(principal);
        return ResponseEntity.ok(adminService.getReports(admin, status, page, size));
    }

    /**
     * DELETE /api/admin/content/{type}/{id}
     * type: post | comment
     */
    @DeleteMapping("/content/{type}/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN', 'MODERATOR')")
    public ResponseEntity<Void> removeContent(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable String type,
            @PathVariable Long   id) {

        adminService.removeContent(resolveAdmin(principal), type, id);
        return ResponseEntity.noContent().build();
    }

    /**
     * PUT /api/admin/reports/{id}/resolve
     */
    @PutMapping("/reports/{id}/resolve")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN', 'MODERATOR')")
    public ResponseEntity<Void> resolveReport(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id) {

        adminService.resolveReport(resolveAdmin(principal), id);
        return ResponseEntity.ok().build();
    }

    /**
     * PUT /api/admin/reports/{id}/dismiss
     */
    @PutMapping("/reports/{id}/dismiss")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN', 'MODERATOR')")
    public ResponseEntity<Void> dismissReport(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id) {

        adminService.dismissReport(resolveAdmin(principal), id);
        return ResponseEntity.ok().build();
    }

    // ── Announcements ─────────────────────────────────────────────────

    /**
     * GET /api/admin/announcements?page=0&size=20
     */
    @GetMapping("/announcements")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN', 'MODERATOR')")
    public ResponseEntity<PagedAdminResponse<AnnouncementResponse>> getAnnouncements(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size) {

        AppUser admin = resolveAdmin(principal);
        return ResponseEntity.ok(adminService.getAnnouncements(admin, page, size));
    }

    /**
     * POST /api/admin/announcements
     * Body: CreateAnnouncementRequest
     */
    @PostMapping("/announcements")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<AnnouncementResponse> createAnnouncement(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody CreateAnnouncementRequest request) {

        AppUser admin = resolveAdmin(principal);
        AnnouncementResponse created = adminService.createAnnouncement(admin, request);
        return ResponseEntity.status(201).body(created);
    }

    /**
     * PUT /api/admin/announcements/{id}/pin
     * Body: { "pinned": true | false }
     */
    @PutMapping("/announcements/{id}/pin")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<AnnouncementResponse> pinAnnouncement(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id,
            @RequestBody Map<String, Boolean> body) {

        boolean pinned = Boolean.TRUE.equals(body.get("pinned"));
        AppUser admin = resolveAdmin(principal);
        return ResponseEntity.ok(adminService.pinAnnouncement(admin, id, pinned));
    }

    /**
     * DELETE /api/admin/announcements/{id}
     */
    @DeleteMapping("/announcements/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<Void> deleteAnnouncement(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id) {

        adminService.deleteAnnouncement(resolveAdmin(principal), id);
        return ResponseEntity.noContent().build();
    }

    // ── Community Settings ────────────────────────────────────────────

    /**
     * GET /api/admin/community/settings
     */
    @GetMapping("/community/settings")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<CommunitySettingsResponse> getCommunitySettings(
            @AuthenticationPrincipal UserPrincipal principal) {

        return ResponseEntity.ok(adminService.getCommunitySettings(resolveAdmin(principal)));
    }

    /**
     * PUT /api/admin/community/settings
     * Partial update — only non-null fields are applied.
     */
    @PutMapping("/community/settings")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<CommunitySettingsResponse> updateCommunitySettings(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestBody UpdateCommunitySettingsRequest request) {

        AppUser admin = resolveAdmin(principal);
        return ResponseEntity.ok(adminService.updateCommunitySettings(admin, request));
    }

    /**
     * POST /api/admin/community/regenerate-invite
     * Rotates the community's invite code. Old links stop working immediately.
     * Returns: { "inviteCode": "NEWCODE" }
     */
    @PostMapping("/community/regenerate-invite")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<Map<String, String>> regenerateInviteCode(
            @AuthenticationPrincipal UserPrincipal principal) {

        return ResponseEntity.ok(adminService.regenerateInviteCode(resolveAdmin(principal)));
    }
}
