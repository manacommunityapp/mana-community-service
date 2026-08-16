package com.manacommunity.api.service.impl;

import com.manacommunity.api.constants.PermissionConstants;
import com.manacommunity.api.model.Community;
import com.manacommunity.api.model.Role;
import com.manacommunity.api.model.RolePermission;
import com.manacommunity.api.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class CommunityRoleInitializer {

    private final RoleRepository roleRepo;

    private static final Map<String, List<String>> ROLE_PERMISSIONS_MAP = Map.ofEntries(
            Map.entry(PermissionConstants.ROLE_ADMIN, PermissionConstants.ADMIN_PERMISSIONS),
            Map.entry(PermissionConstants.ROLE_COMMUNITY_FEED_ADMIN, PermissionConstants.COMMUNITY_FEED_ADMIN_PERMISSIONS),
            Map.entry(PermissionConstants.ROLE_SPORTS_ADMIN, PermissionConstants.SPORTS_ADMIN_PERMISSIONS),
            Map.entry(PermissionConstants.ROLE_MARKETPLACE_ADMIN, PermissionConstants.MARKETPLACE_ADMIN_PERMISSIONS),
            Map.entry(PermissionConstants.ROLE_VISITORS_ADMIN, PermissionConstants.VISITORS_ADMIN_PERMISSIONS),
            Map.entry(PermissionConstants.ROLE_NOTICES_ADMIN, PermissionConstants.NOTICES_ADMIN_PERMISSIONS),
            Map.entry(PermissionConstants.ROLE_BOOKINGS_ADMIN, PermissionConstants.BOOKINGS_ADMIN_PERMISSIONS),
            Map.entry(PermissionConstants.ROLE_HELPDESK_ADMIN, PermissionConstants.HELPDESK_ADMIN_PERMISSIONS),
            Map.entry(PermissionConstants.ROLE_POLLS_ADMIN, PermissionConstants.POLLS_ADMIN_PERMISSIONS),
            Map.entry(PermissionConstants.ROLE_JOBS_ADMIN, PermissionConstants.JOBS_ADMIN_PERMISSIONS),
            Map.entry(PermissionConstants.ROLE_EVENTS_ADMIN, PermissionConstants.EVENTS_ADMIN_PERMISSIONS),
            Map.entry(PermissionConstants.ROLE_COMMUNITY_MGMT_ADMIN, PermissionConstants.COMMUNITY_MGMT_ADMIN_PERMISSIONS),
            Map.entry(PermissionConstants.ROLE_FINANCE_MGMT_ADMIN, PermissionConstants.FINANCE_MGMT_ADMIN_PERMISSIONS),
            Map.entry(PermissionConstants.ROLE_ADMIN_HUB_ADMIN, PermissionConstants.ADMIN_HUB_ADMIN_PERMISSIONS),
            Map.entry(PermissionConstants.ROLE_FOOD_OS_ADMIN, PermissionConstants.FOOD_OS_ADMIN_PERMISSIONS),
            Map.entry(PermissionConstants.ROLE_VENDOR_MANAGEMENT_ADMIN, PermissionConstants.VENDOR_MANAGEMENT_ADMIN_PERMISSIONS),
            Map.entry(PermissionConstants.ROLE_MEMBER, PermissionConstants.MEMBER_PERMISSIONS),
            Map.entry(PermissionConstants.ROLE_VENDOR, PermissionConstants.VENDOR_PERMISSIONS),
            Map.entry(PermissionConstants.ROLE_CASHIER, PermissionConstants.CASHIER_PERMISSIONS),
            Map.entry(PermissionConstants.ROLE_STAFF, PermissionConstants.STAFF_PERMISSIONS),
            Map.entry(PermissionConstants.ROLE_USER, PermissionConstants.USER_PERMISSIONS)
    );

    @Transactional
    public void initializeCommunityRoles(Community community) {
        if (community == null || community.getId() == null) {
            log.warn("Cannot initialize roles: community or community ID is null");
            return;
        }

        log.info("Initializing baseline roles for community: {} (ID: {})", community.getName(), community.getId());
        Long communityId = community.getId();

        for (Map.Entry<String, List<String>> entry : ROLE_PERMISSIONS_MAP.entrySet()) {
            String roleName = entry.getKey();
            List<String> perms = entry.getValue();

            // Check if this community role already exists
            boolean exists = roleRepo.existsByNameIgnoreCaseAndCommunityId(roleName, communityId);
            if (!exists) {
                Role role = Role.builder()
                        .name(roleName)
                        .communityId(communityId)
                        .permissions(new HashSet<>())
                        .build();

                for (String p : perms) {
                    RolePermission rp = RolePermission.builder()
                            .role(roleName)
                            .permissionKey(p)
                            .roleEntity(role)
                            .build();
                    role.getPermissions().add(rp);
                }

                roleRepo.save(role);
                log.info("Created community-scoped role: {} for community ID: {}", roleName, communityId);
            }
        }
    }
}
