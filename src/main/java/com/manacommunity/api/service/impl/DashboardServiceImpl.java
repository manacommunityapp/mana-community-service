package com.manacommunity.api.service.impl;

import com.manacommunity.api.dto.dashboard.AdminDashboardStatsResponse;
import com.manacommunity.api.dto.dashboard.UserDashboardStatsResponse;
import com.manacommunity.api.events.repository.CommunityEventRepository;
import com.manacommunity.api.noticeboard.repository.NoticeRepository;
import com.manacommunity.api.repository.CommunityRepository;
import com.manacommunity.api.repository.RoleRepository;
import com.manacommunity.api.service.DashboardService;
import com.manacommunity.api.user.model.AppUser;
import com.manacommunity.api.user.repository.AppUserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class DashboardServiceImpl implements DashboardService {

    private final AppUserRepository appUserRepository;
    private final RoleRepository roleRepository;
    private final CommunityRepository communityRepository;
    private final CommunityEventRepository communityEventRepository;
    private final NoticeRepository noticeRepository;

    @Override
    @Transactional(readOnly = true)
    public AdminDashboardStatsResponse getAdminStats(AppUser caller) {
        Long communityId = caller.getCommunity() != null ? caller.getCommunity().getId() : null;

        long totalUsers = communityId != null
                ? appUserRepository.countByCommunityId(communityId)
                : appUserRepository.count();

        long pendingKyc = communityId != null
                ? appUserRepository.countByCommunityIdAndKycStatus(communityId, "PENDING")
                : appUserRepository.countByKycStatus("PENDING");

        long verifiedUsers = communityId != null
                ? appUserRepository.countByCommunityIdAndKycStatus(communityId, "VERIFIED")
                : appUserRepository.countByKycStatus("VERIFIED");

        long totalRoles = roleRepository.count();
        long totalCommunities = communityRepository.count();

        return AdminDashboardStatsResponse.builder()
                .totalUsers(totalUsers)
                .pendingKycCount(pendingKyc)
                .verifiedUsersCount(verifiedUsers)
                .totalRolesCount(totalRoles)
                .totalCommunitiesCount(totalCommunities)
                .recentActivities(Collections.emptyList())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public UserDashboardStatsResponse getUserStats(AppUser caller) {
        Long communityId = caller.getCommunity() != null ? caller.getCommunity().getId() : null;
        String communityName = caller.getCommunity() != null ? caller.getCommunity().getName() : "Community";

        long activeEvents = communityId != null
                ? communityEventRepository.countByCommunityId(communityId)
                : 0L;

        long activeNotices = communityId != null
                ? noticeRepository.countByCommunityId(communityId)
                : 0L;

        return UserDashboardStatsResponse.builder()
                .userName(caller.getFullName() != null ? caller.getFullName() : caller.getEmail())
                .communityName(communityName)
                .activeEventsCount(activeEvents)
                .activeNoticesCount(activeNotices)
                .myBookingsCount(0L)
                .myTicketsCount(0L)
                .recentNotices(Collections.emptyList())
                .build();
    }
}
