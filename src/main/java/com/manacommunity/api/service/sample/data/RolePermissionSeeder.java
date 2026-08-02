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

import java.util.List;

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
        List<String> rolesToSeed = List.of(ROLE_SUPER_ADMIN, ROLE_ADMIN, ROLE_SPORTS_ADMIN, ROLE_MEMBER, ROLE_VENDOR, ROLE_CASHIER, ROLE_STAFF);
        for (String roleName : rolesToSeed) {
            if (!roleRepo.existsByNameIgnoreCaseAndCommunityIdIsNull(roleName)) {
                roleRepo.save(Role.builder().name(roleName.toUpperCase()).build());
            }
        }

        saveRolePermissions(ROLE_SUPER_ADMIN, ALL_PERMISSIONS);
        saveRolePermissions(ROLE_ADMIN, ADMIN_PERMISSIONS);
        saveRolePermissions(ROLE_SPORTS_ADMIN, SPORTS_ADMIN_PERMISSIONS);
        saveRolePermissions(ROLE_MEMBER, MEMBER_PERMISSIONS);
        saveRolePermissions(ROLE_VENDOR, VENDOR_PERMISSIONS);
        saveRolePermissions(ROLE_CASHIER, CASHIER_PERMISSIONS);
        saveRolePermissions(ROLE_STAFF, STAFF_PERMISSIONS);

        log.info("✓ Role permissions seeded successfully");
    }


    @Transactional
    public void seed() {
        log.info("Seeding role permissions...");

        List<String> rolesToSeed = List.of(ROLE_SUPER_ADMIN, ROLE_ADMIN, ROLE_SPORTS_ADMIN, ROLE_MEMBER, ROLE_VENDOR, ROLE_CASHIER, ROLE_STAFF);
        for (String roleName : rolesToSeed) {
            if (!roleRepo.existsByNameIgnoreCaseAndCommunityIdIsNull(roleName)) {
                roleRepo.save(Role.builder().name(roleName.toUpperCase()).build());
            }
        }

        saveRolePermissions(ROLE_SUPER_ADMIN, ALL_PERMISSIONS);
        saveRolePermissions(ROLE_ADMIN, ADMIN_PERMISSIONS);
        saveRolePermissions(ROLE_SPORTS_ADMIN, SPORTS_ADMIN_PERMISSIONS);
        saveRolePermissions(ROLE_MEMBER, MEMBER_PERMISSIONS);
        saveRolePermissions(ROLE_VENDOR, VENDOR_PERMISSIONS);

        saveRolePermissions(ROLE_CASHIER, CASHIER_PERMISSIONS);
        saveRolePermissions(ROLE_STAFF, STAFF_PERMISSIONS);

        log.info("✓ Role permissions seeded successfully");
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
        log.info("Seeding user-specific permissions for Sunil...");
        appUserRepo.findByEmail("sunil@gmail.com").ifPresent(sunil -> {
            Role adminRole = sunil.getRoleEntity();
            if (adminRole != null) {
                List<RolePermission> existingPerms = new java.util.ArrayList<>(rolePermissionRepo.findByUserId(sunil.getId()));
                for (String perm : ADMIN_PERMISSIONS.stream().distinct().toList()) {
                    boolean exists = existingPerms.stream()
                            .anyMatch(rp -> rp.getPermissionKey().equalsIgnoreCase(perm)
                                    && rp.getRoleEntity() != null
                                    && rp.getRoleEntity().getId().equals(adminRole.getId()));
                    if (!exists) {
                        RolePermission rp = RolePermission.builder()
                                .role("ADMIN")
                                .roleEntity(adminRole)
                                .permissionKey(perm)
                                .user(sunil)
                                .build();
                        rolePermissionRepo.save(rp);
                        existingPerms.add(rp);
                    }
                }
                log.info("✓ User-specific permissions seeded for Sunil (ADMIN)");
            }
        });
    }
}
