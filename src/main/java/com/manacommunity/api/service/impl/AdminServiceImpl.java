package com.manacommunity.api.service.impl;

import com.manacommunity.api.dto.admin.*;
import com.manacommunity.api.exception.ResourceNotFoundException;
import com.manacommunity.api.exception.UnauthorizedActionException;
import com.manacommunity.api.model.*;
import com.manacommunity.api.repository.*;
import com.manacommunity.api.service.AdminService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminServiceImpl implements AdminService {

    private final AppUserRepository       userRepo;
    private final PostRepository          postRepo;
    private final ContentReportRepository reportRepo;
    private final AnnouncementRepository  announcementRepo;
    private final CommunityRepository     communityRepo;
    private final CommunitySettingsRepository settingsRepo;

    // ── Helpers ───────────────────────────────────────────────────────

    private Long requireCommunityId(AppUser admin) {
        if (admin.getCommunity() == null) {
            throw new UnauthorizedActionException("Admin is not associated with any community.");
        }
        return admin.getCommunity().getId();
    }

    private Community requireCommunity(AppUser admin) {
        Long cid = requireCommunityId(admin);
        return communityRepo.findById(cid)
                .orElseThrow(() -> new ResourceNotFoundException("Community", "id", String.valueOf(cid)));
    }

    private AppUser requireMemberInCommunity(Long userId, Long communityId) {
        AppUser target = userRepo.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", String.valueOf(userId)));
        if (target.getCommunity() == null || !target.getCommunity().getId().equals(communityId)) {
            throw new UnauthorizedActionException("User does not belong to your community.");
        }
        return target;
    }

    // ── Stats ─────────────────────────────────────────────────────────

    @Override
    public AdminStatsResponse getStats(AppUser admin) {
        Long cid = requireCommunityId(admin);

        // Member counts by status
        List<AppUser> allMembers = userRepo.findByCommunityId(cid);
        long total     = allMembers.size();
        long pending   = allMembers.stream().filter(u -> "PENDING".equalsIgnoreCase(u.getKycStatus())).count();
        long active    = allMembers.stream().filter(u ->
                Boolean.TRUE.equals(u.getIsActive()) && "VERIFIED".equalsIgnoreCase(u.getKycStatus())).count();
        long suspended = allMembers.stream().filter(u -> Boolean.FALSE.equals(u.getIsActive())).count();

        // New members this month
        LocalDateTime monthStart = LocalDate.now().withDayOfMonth(1).atStartOfDay();
        long newThisMonth = allMembers.stream()
                .filter(u -> u.getCreatedAt() != null && u.getCreatedAt().isAfter(monthStart))
                .count();

        // Posts
        long totalPosts = postRepo.countByCommunityId(cid);
        LocalDateTime todayStart = LocalDate.now().atStartOfDay();
        long postsToday = postRepo.countByCommunityIdAndCreatedAtAfter(cid, todayStart);

        // Reports
        long pendingReports = reportRepo.countByCommunityIdAndStatus(cid, "PENDING");

        return AdminStatsResponse.builder()
                .totalMembers(total)
                .pendingApprovals(pending)
                .activeMembers(active)
                .suspendedMembers(suspended)
                .postsToday(postsToday)
                .totalPosts(totalPosts)
                .eventsThisWeek(0L)   // wire up EventRepository when available
                .pendingReports(pendingReports)
                .newMembersThisMonth(newThisMonth)
                .build();
    }

    // ── Members ───────────────────────────────────────────────────────

    @Override
    public PagedAdminResponse<AdminMemberResponse> getMembers(
            AppUser admin, String status, String search, int page, int size) {

        Long cid = requireCommunityId(admin);
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.min(size, 50),
                Sort.by("fullName").ascending());

        // Fetch the right page from the DB
        Page<AppUser> userPage;

        if (search != null && !search.isBlank()) {
            // Search with optional status filter
            List<AppUser> searched = userRepo
                    .findByCommunityIdAndFullNameContainingIgnoreCase(cid, search.trim());
            searched = filterByStatus(searched, status);
            int start = (int) pageable.getOffset();
            int end   = Math.min(start + pageable.getPageSize(), searched.size());
            List<AppUser> slice = start > searched.size() ? List.of() : searched.subList(start, end);
            userPage = new PageImpl<>(slice, pageable, searched.size());
        } else {
            // No search — pull from DB with status filter
            userPage = fetchByStatus(cid, status, pageable);
        }

        return PagedAdminResponse.from(userPage, AdminMemberResponse::from);
    }

    private List<AppUser> filterByStatus(List<AppUser> users, String status) {
        if (status == null || status.isBlank() || "ALL".equalsIgnoreCase(status)) return users;
        return switch (status.toUpperCase()) {
            case "PENDING"   -> users.stream().filter(u -> "PENDING".equalsIgnoreCase(u.getKycStatus())).collect(Collectors.toList());
            case "ACTIVE"    -> users.stream().filter(u -> Boolean.TRUE.equals(u.getIsActive()) && "VERIFIED".equalsIgnoreCase(u.getKycStatus())).collect(Collectors.toList());
            case "SUSPENDED" -> users.stream().filter(u -> Boolean.FALSE.equals(u.getIsActive())).collect(Collectors.toList());
            default          -> users;
        };
    }

    private Page<AppUser> fetchByStatus(Long communityId, String status, Pageable pageable) {
        if (status == null || status.isBlank() || "ALL".equalsIgnoreCase(status)) {
            return userRepo.findByCommunityId(communityId, pageable);
        }
        // Filter in memory from full community list (acceptable for community sizes)
        List<AppUser> all = userRepo.findByCommunityId(communityId);
        List<AppUser> filtered = filterByStatus(all, status);
        int start = (int) pageable.getOffset();
        int end   = Math.min(start + pageable.getPageSize(), filtered.size());
        List<AppUser> slice = start > filtered.size() ? List.of() : filtered.subList(start, end);
        return new PageImpl<>(slice, pageable, filtered.size());
    }

    @Override
    @Transactional
    public void approveMember(AppUser admin, Long userId) {
        Long cid    = requireCommunityId(admin);
        AppUser target = requireMemberInCommunity(userId, cid);
        target.setKycStatus("VERIFIED");
        target.setIsActive(true);
        userRepo.save(target);
        log.info("Admin {} approved member {}", admin.getId(), userId);
    }

    @Override
    @Transactional
    public void rejectMember(AppUser admin, Long userId, String reason) {
        Long cid    = requireCommunityId(admin);
        AppUser target = requireMemberInCommunity(userId, cid);
        target.setKycStatus("REJECTED");
        target.setIsActive(false);
        userRepo.save(target);
        log.info("Admin {} rejected member {} — reason: {}", admin.getId(), userId, reason);
    }

    @Override
    @Transactional
    public void suspendMember(AppUser admin, Long userId, String reason) {
        Long cid    = requireCommunityId(admin);
        AppUser target = requireMemberInCommunity(userId, cid);
        // Prevent suspending another ADMIN
        if ("ADMIN".equalsIgnoreCase(target.getRole()) || "SUPER_ADMIN".equalsIgnoreCase(target.getRole())) {
            throw new UnauthorizedActionException("Cannot suspend another admin.");
        }
        target.setIsActive(false);
        userRepo.save(target);
        log.info("Admin {} suspended member {} — reason: {}", admin.getId(), userId, reason);
    }

    @Override
    @Transactional
    public void activateMember(AppUser admin, Long userId) {
        Long cid    = requireCommunityId(admin);
        AppUser target = requireMemberInCommunity(userId, cid);
        target.setIsActive(true);
        if (!"VERIFIED".equalsIgnoreCase(target.getKycStatus())) {
            target.setKycStatus("VERIFIED");
        }
        userRepo.save(target);
        log.info("Admin {} activated member {}", admin.getId(), userId);
    }

    @Override
    @Transactional
    public void changeMemberRole(AppUser admin, Long userId, String role) {
        Long cid    = requireCommunityId(admin);
        AppUser target = requireMemberInCommunity(userId, cid);
        target.setRole(role.toUpperCase());
        userRepo.save(target);
        log.info("Admin {} changed role of {} to {}", admin.getId(), userId, role);
    }

    // ── Content Moderation ────────────────────────────────────────────

    @Override
    public PagedAdminResponse<ReportResponse> getReports(
            AppUser admin, String status, int page, int size) {

        Long cid = requireCommunityId(admin);
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.min(size, 50),
                Sort.by("createdAt").descending());

        Page<ContentReport> reports;
        if (status == null || status.isBlank() || "ALL".equalsIgnoreCase(status)) {
            reports = reportRepo.findByCommunityId(cid, pageable);
        } else {
            reports = reportRepo.findByCommunityIdAndStatus(cid, status.toUpperCase(), pageable);
        }

        return PagedAdminResponse.from(reports, ReportResponse::from);
    }

    @Override
    @Transactional
    public void removeContent(AppUser admin, String targetType, Long targetId) {
        Long cid = requireCommunityId(admin);
        if ("POST".equalsIgnoreCase(targetType)) {
            Post post = postRepo.findById(targetId)
                    .orElseThrow(() -> new ResourceNotFoundException("Post", "id", String.valueOf(targetId)));
            if (!post.getCommunity().getId().equals(cid)) {
                throw new UnauthorizedActionException("Post does not belong to your community.");
            }
            postRepo.delete(post);
            log.info("Admin {} removed post {}", admin.getId(), targetId);
        }
        // COMMENT removal: add CommentRepository dependency when ready
    }

    @Override
    @Transactional
    public void resolveReport(AppUser admin, Long reportId) {
        ContentReport report = reportRepo.findById(reportId)
                .orElseThrow(() -> new ResourceNotFoundException("Report", "id", String.valueOf(reportId)));
        requireCommunityId(admin); // ensure admin belongs to a community
        report.setStatus("RESOLVED");
        report.setResolvedBy(admin);
        report.setResolvedAt(LocalDateTime.now());
        reportRepo.save(report);
    }

    @Override
    @Transactional
    public void dismissReport(AppUser admin, Long reportId) {
        ContentReport report = reportRepo.findById(reportId)
                .orElseThrow(() -> new ResourceNotFoundException("Report", "id", String.valueOf(reportId)));
        requireCommunityId(admin);
        report.setStatus("DISMISSED");
        report.setResolvedBy(admin);
        report.setResolvedAt(LocalDateTime.now());
        reportRepo.save(report);
    }

    // ── Announcements ─────────────────────────────────────────────────

    @Override
    public PagedAdminResponse<AnnouncementResponse> getAnnouncements(AppUser admin, int page, int size) {
        Long cid = requireCommunityId(admin);
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.min(size, 50));
        Page<Announcement> items = announcementRepo
                .findByCommunityIdOrderByPinnedDescCreatedAtDesc(cid, pageable);
        return PagedAdminResponse.from(items, AnnouncementResponse::from);
    }

    @Override
    @Transactional
    public AnnouncementResponse createAnnouncement(AppUser admin, CreateAnnouncementRequest req) {
        Community community = requireCommunity(admin);

        LocalDateTime expiresAt = null;
        if (req.getExpiresAt() != null && !req.getExpiresAt().isBlank()) {
            expiresAt = LocalDateTime.parse(req.getExpiresAt(), DateTimeFormatter.ISO_DATE_TIME);
        }

        String priority = "URGENT".equalsIgnoreCase(req.getPriority()) ? "URGENT" : "NORMAL";

        Announcement ann = Announcement.builder()
                .community(community)
                .author(admin)
                .title(req.getTitle().trim())
                .content(req.getContent().trim())
                .priority(priority)
                .pinned(req.isPinned())
                .expiresAt(expiresAt)
                .build();

        ann = announcementRepo.save(ann);
        log.info("Admin {} created announcement '{}' (priority={})", admin.getId(), ann.getTitle(), priority);
        return AnnouncementResponse.from(ann);
    }

    @Override
    @Transactional
    public AnnouncementResponse pinAnnouncement(AppUser admin, Long announcementId, boolean pinned) {
        Long cid = requireCommunityId(admin);
        Announcement ann = announcementRepo.findById(announcementId)
                .orElseThrow(() -> new ResourceNotFoundException("Announcement", "id", String.valueOf(announcementId)));
        if (!ann.getCommunity().getId().equals(cid)) {
            throw new UnauthorizedActionException("Announcement does not belong to your community.");
        }
        ann.setPinned(pinned);
        return AnnouncementResponse.from(announcementRepo.save(ann));
    }

    @Override
    @Transactional
    public void deleteAnnouncement(AppUser admin, Long announcementId) {
        Long cid = requireCommunityId(admin);
        Announcement ann = announcementRepo.findById(announcementId)
                .orElseThrow(() -> new ResourceNotFoundException("Announcement", "id", String.valueOf(announcementId)));
        if (!ann.getCommunity().getId().equals(cid)) {
            throw new UnauthorizedActionException("Announcement does not belong to your community.");
        }
        announcementRepo.delete(ann);
        log.info("Admin {} deleted announcement {}", admin.getId(), announcementId);
    }

    // ── Community Settings ────────────────────────────────────────────

    @Override
    public CommunitySettingsResponse getCommunitySettings(AppUser admin) {
        Community community = requireCommunity(admin);
        CommunitySettings settings = settingsRepo
                .findByCommunityId(community.getId())
                .orElse(null);
        return CommunitySettingsResponse.from(community, settings);
    }

    @Override
    @Transactional
    public CommunitySettingsResponse updateCommunitySettings(AppUser admin, UpdateCommunitySettingsRequest req) {
        Community community = requireCommunity(admin);

        // Update the core Community fields
        if (req.getName() != null && !req.getName().isBlank()) {
            community.setName(req.getName().trim());
        }
        if (req.getCity() != null)  community.setCity(req.getCity().trim());
        if (req.getState() != null) community.setState(req.getState().trim());
        communityRepo.save(community);

        // Upsert the CommunitySettings row
        CommunitySettings settings = settingsRepo
                .findByCommunityId(community.getId())
                .orElseGet(() -> CommunitySettings.builder().community(community).build());

        if (req.getDescription() != null) settings.setDescription(req.getDescription().trim());
        if (req.getAddress()     != null) settings.setAddress(req.getAddress().trim());
        if (req.getMaxMembers()  != null) settings.setMaxMembers(req.getMaxMembers());

        if (req.getFeatures() != null) {
            UpdateCommunitySettingsRequest.Features f = req.getFeatures();
            if (f.getMarketplace() != null) settings.setFeatureMarketplace(f.getMarketplace());
            if (f.getSports()      != null) settings.setFeatureSports(f.getSports());
            if (f.getAuction()     != null) settings.setFeatureAuction(f.getAuction());
            if (f.getJobs()        != null) settings.setFeatureJobs(f.getJobs());
            if (f.getPolls()       != null) settings.setFeaturePolls(f.getPolls());
        }

        settingsRepo.save(settings);
        log.info("Admin {} updated community settings for community {}", admin.getId(), community.getId());
        return CommunitySettingsResponse.from(community, settings);
    }

    @Override
    @Transactional
    public Map<String, String> regenerateInviteCode(AppUser admin) {
        Community community = requireCommunity(admin);
        String newCode = generateInviteCode();
        community.setInviteCode(newCode);
        communityRepo.save(community);
        log.info("Admin {} regenerated invite code for community {}", admin.getId(), community.getId());
        return Map.of("inviteCode", newCode);
    }

    // ── Utilities ─────────────────────────────────────────────────────

    private String generateInviteCode() {
        // 8-character alphanumeric code — readable and easy to share
        String chars   = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";   // no O/0/I/1 ambiguity
        Random rng     = new Random();
        StringBuilder sb = new StringBuilder(8);
        for (int i = 0; i < 8; i++) {
            sb.append(chars.charAt(rng.nextInt(chars.length())));
        }
        return sb.toString();
    }
}
