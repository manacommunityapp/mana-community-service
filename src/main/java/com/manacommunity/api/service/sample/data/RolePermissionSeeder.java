package com.manacommunity.api.service.sample.data;

import com.manacommunity.api.user.model.AppUser;
import com.manacommunity.api.model.Role;
import com.manacommunity.api.model.RolePermission;
import com.manacommunity.api.user.repository.AppUserRepository;
import com.manacommunity.api.repository.RolePermissionRepository;
import com.manacommunity.api.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static com.manacommunity.api.constants.PermissionConstants.*;

/**
 * RolePermissionSeeder — Handles seeding baseline roles and their respective permissions.
 * All permission keys are sourced from {@link com.manacommunity.api.constants.PermissionConstants}.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RolePermissionSeeder {

    private final RoleRepository roleRepo;
    private final RolePermissionRepository rolePermissionRepo;
    private final AppUserRepository appUserRepo;

    @Transactional
    public void defaultSeed() {
        log.info("Seeding role permissions...");

        // First, ensure all roles exist in the roles table (global/system roles)
        List<String> rolesToSeed = List.of(
                ROLE_SUPER_ADMIN, ROLE_ADMIN, ROLE_COMMUNITY_ADMIN,
                ROLE_COMMUNITY_FEED_ADMIN, ROLE_SPORTS_ADMIN, ROLE_MARKETPLACE_ADMIN,
                ROLE_VISITORS_ADMIN, ROLE_NOTICES_ADMIN, ROLE_BOOKINGS_ADMIN,
                ROLE_HELPDESK_ADMIN, ROLE_POLLS_ADMIN, ROLE_JOBS_ADMIN,
                ROLE_EVENTS_ADMIN, ROLE_COMMUNITY_MGMT_ADMIN,
                ROLE_FINANCE_MGMT_ADMIN, ROLE_ADMIN_HUB_ADMIN, ROLE_FOOD_OS_ADMIN,
                ROLE_VENDOR_MANAGEMENT_ADMIN, ROLE_MEMBER, ROLE_VENDOR,
                ROLE_CASHIER, ROLE_STAFF, ROLE_USER
        );
        for (String roleName : rolesToSeed) {
            if (!roleRepo.existsByNameIgnoreCaseAndCommunityIdIsNull(roleName)) {
                roleRepo.save(Role.builder().name(roleName.toUpperCase()).build());
            }
        }

        saveRolePermissions(ROLE_SUPER_ADMIN, ALL_PERMISSIONS);
        saveRolePermissions(ROLE_ADMIN, ADMIN_PERMISSIONS);
        saveRolePermissions(ROLE_COMMUNITY_ADMIN, ADMIN_PERMISSIONS);
        saveRolePermissions(ROLE_COMMUNITY_FEED_ADMIN, COMMUNITY_FEED_ADMIN_PERMISSIONS);
        saveRolePermissions(ROLE_SPORTS_ADMIN, SPORTS_ADMIN_PERMISSIONS);
        saveRolePermissions(ROLE_MARKETPLACE_ADMIN, MARKETPLACE_ADMIN_PERMISSIONS);
        saveRolePermissions(ROLE_VISITORS_ADMIN, VISITORS_ADMIN_PERMISSIONS);
        saveRolePermissions(ROLE_NOTICES_ADMIN, NOTICES_ADMIN_PERMISSIONS);
        saveRolePermissions(ROLE_BOOKINGS_ADMIN, BOOKINGS_ADMIN_PERMISSIONS);
        saveRolePermissions(ROLE_HELPDESK_ADMIN, HELPDESK_ADMIN_PERMISSIONS);
        saveRolePermissions(ROLE_POLLS_ADMIN, POLLS_ADMIN_PERMISSIONS);
        saveRolePermissions(ROLE_JOBS_ADMIN, JOBS_ADMIN_PERMISSIONS);
        saveRolePermissions(ROLE_EVENTS_ADMIN, EVENTS_ADMIN_PERMISSIONS);
        saveRolePermissions(ROLE_COMMUNITY_MGMT_ADMIN, COMMUNITY_MGMT_ADMIN_PERMISSIONS);
        saveRolePermissions(ROLE_FINANCE_MGMT_ADMIN, FINANCE_MGMT_ADMIN_PERMISSIONS);
        saveRolePermissions(ROLE_ADMIN_HUB_ADMIN, ADMIN_HUB_ADMIN_PERMISSIONS);
        saveRolePermissions(ROLE_FOOD_OS_ADMIN, FOOD_OS_ADMIN_PERMISSIONS);
        saveRolePermissions(ROLE_VENDOR_MANAGEMENT_ADMIN, VENDOR_MANAGEMENT_ADMIN_PERMISSIONS);
        saveRolePermissions(ROLE_MEMBER, MEMBER_PERMISSIONS);
        saveRolePermissions(ROLE_VENDOR, VENDOR_PERMISSIONS);
        saveRolePermissions(ROLE_CASHIER, CASHIER_PERMISSIONS);
        saveRolePermissions(ROLE_STAFF, STAFF_PERMISSIONS);
        saveRolePermissions(ROLE_USER, USER_PERMISSIONS);

        log.info("✓ Role permissions seeded successfully");
    }


    @Transactional
    public void seed() {
        defaultSeed();
    }

    private void saveRolePermissions(String role, List<String> permissions) {
        Role roleEntity = roleRepo.findByNameIgnoreCaseAndCommunityIdIsNull(role)
                .orElseThrow(() -> new IllegalStateException("Global Role " + role + " not found"));
        // Fetch existing permissions once per role instead of calling findAll() per permission
        List<RolePermission> existingPerms = new java.util.ArrayList<>(rolePermissionRepo.findByRoleIgnoreCase(role));
        for (String perm : permissions.stream().distinct().toList()) {
            boolean exists = existingPerms.stream()
                    .anyMatch(rp -> rp.getPermissionKey().equalsIgnoreCase(perm)
                            && rp.getUser() == null
                            && rp.getRoleEntity() != null
                            && rp.getRoleEntity().getId().equals(roleEntity.getId()));
            if (!exists) {
                RolePermission rp = RolePermission.builder()
                        .role(role.toUpperCase())
                        .roleEntity(roleEntity)
                        .permissionKey(perm)
                        .build();
                rolePermissionRepo.save(rp);
                existingPerms.add(rp);
            }
        }
    }

    @Transactional
    public void seedUserPermissions() {
        log.info("Seeding user-specific permissions for all LE community users...");

        List<String> leUserEmails = List.of(
                "sandeep60.kamarapu@gmail.com",
                "kskreddy1989@gmail.com",
                "chetan.velmareddy@gmail.com",
                "ramesh@gmail.com",
                "mady@gmail.com",
                "kusivarshitha23@gmail.com",
                "Bhupal@gmail.com"
        );

        for (String email : leUserEmails) {
            appUserRepo.findByEmail(email).ifPresent(this::seedPermissionsForUser);
        }

        log.info("✓ User-specific permissions seeded for all LE community users");
    }

    private void seedPermissionsForUser(AppUser user) {
        Set<Role> roles = user.getUserRoles();
        if (roles == null || roles.isEmpty()) return;

        Set<String> mergedPermissions = new LinkedHashSet<>();
        for (Role role : roles) {
            mergedPermissions.addAll(getPermissionsForRole(role.getName()));
        }
        if (mergedPermissions.isEmpty()) return;

        List<RolePermission> existingPerms = new ArrayList<>(rolePermissionRepo.findByUserId(user.getId()));
        Role primaryRole = user.getRoleEntity();
        int created = 0;

        for (String perm : mergedPermissions) {
            boolean exists = existingPerms.stream()
                    .anyMatch(rp -> rp.getPermissionKey().equalsIgnoreCase(perm));
            if (!exists) {
                RolePermission rp = RolePermission.builder()
                        .role(primaryRole != null ? primaryRole.getName() : ROLE_USER)
                        .roleEntity(primaryRole)
                        .permissionKey(perm)
                        .user(user)
                        .build();
                rolePermissionRepo.save(rp);
                existingPerms.add(rp);
                created++;
            }
        }

        log.info("  → {} : {} permissions seeded ({} roles)", user.getEmail(), created,
                roles.stream().map(Role::getName).toList());
    }
}
