package com.manacommunity.api.service;

import com.manacommunity.api.dto.admin.*;
import com.manacommunity.api.user.model.AppUser;

import java.util.Map;

public interface AdminService {

    // ── Dashboard ────────────────────────────────────────────────────
    AdminStatsResponse getStats(AppUser admin);

    // ── Members ──────────────────────────────────────────────────────
    PagedAdminResponse<AdminMemberResponse> getMembers(
            AppUser admin, String status, String search, int page, int size);

    void approveMember(AppUser admin, Long userId);

    void rejectMember(AppUser admin, Long userId, String reason);

    void suspendMember(AppUser admin, Long userId, String reason);

    void activateMember(AppUser admin, Long userId);

    void changeMemberRole(AppUser admin, Long userId, String role);

    // ── Content Moderation ───────────────────────────────────────────
    PagedAdminResponse<ReportResponse> getReports(
            AppUser admin, String status, int page, int size);

    void removeContent(AppUser admin, String targetType, Long targetId);

    void resolveReport(AppUser admin, Long reportId);

    void dismissReport(AppUser admin, Long reportId);

    // ── Announcements ────────────────────────────────────────────────
    PagedAdminResponse<AnnouncementResponse> getAnnouncements(AppUser admin, int page, int size);

    AnnouncementResponse createAnnouncement(AppUser admin, CreateAnnouncementRequest request);

    AnnouncementResponse pinAnnouncement(AppUser admin, Long announcementId, boolean pinned);

    void deleteAnnouncement(AppUser admin, Long announcementId);

    // ── Community Settings ───────────────────────────────────────────
    CommunitySettingsResponse getCommunitySettings(AppUser admin);

    CommunitySettingsResponse updateCommunitySettings(AppUser admin, UpdateCommunitySettingsRequest request);

    Map<String, String> regenerateInviteCode(AppUser admin);
}
