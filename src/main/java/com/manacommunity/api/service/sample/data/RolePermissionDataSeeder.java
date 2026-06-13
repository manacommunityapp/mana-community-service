package com.manacommunity.api.service.sample.data;

import com.manacommunity.api.model.Role;
import com.manacommunity.api.model.RolePermission;
import com.manacommunity.api.repository.RolePermissionRepository;
import com.manacommunity.api.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static com.manacommunity.api.constants.PermissionConstants.MEMBER_PERMISSIONS;

/**
 * RolePermissionDataSeeder — dedicated seeder for the {@code role_permissions}
 * table. Seeds a distinct global role ({@code MODERATOR}) with a baseline
 * permission set (idempotent) so it coexists with {@link RolePermissionSeeder}.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RolePermissionDataSeeder {

    public static final String ROLE_NAME = "MODERATOR";

    private final RoleRepository roleRepo;
    private final RolePermissionRepository rolePermissionRepo;

    @Transactional
    public void seed() {
        log.info("Seeding role_permissions table sample data...");

        if (!roleRepo.existsByNameIgnoreCaseAndCommunityIdIsNull(ROLE_NAME)) {
            roleRepo.save(Role.builder().name(ROLE_NAME).build());
        }

        Role roleEntity = roleRepo.findByNameIgnoreCaseAndCommunityIdIsNull(ROLE_NAME)
                .orElseThrow(() -> new IllegalStateException("Global Role " + ROLE_NAME + " not found"));

        List<RolePermission> existingPerms = rolePermissionRepo.findByRoleIgnoreCase(ROLE_NAME);
        int created = 0;
        for (String perm : MEMBER_PERMISSIONS) {
            boolean exists = existingPerms.stream().anyMatch(rp ->
                    rp.getPermissionKey().equalsIgnoreCase(perm)
                            && rp.getUser() == null
                            && rp.getRoleEntity() != null
                            && rp.getRoleEntity().getId().equals(roleEntity.getId()));
            if (!exists) {
                rolePermissionRepo.save(RolePermission.builder()
                        .role(ROLE_NAME)
                        .roleEntity(roleEntity)
                        .permissionKey(perm)
                        .build());
                created++;
            }
        }
        log.info("✓ Role permissions table seeded: {} permission(s) for {}", created, ROLE_NAME);
    }
}
