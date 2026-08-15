package com.manacommunity.api.user.service.impl;

import static com.manacommunity.api.constants.PermissionConstants.*;

import com.manacommunity.api.exception.DuplicateResourceException;
import com.manacommunity.api.exception.InvalidInputException;
import com.manacommunity.api.exception.InvalidInviteCodeException;
import com.manacommunity.api.exception.ResourceNotFoundException;
import com.manacommunity.api.model.Community;
import com.manacommunity.api.model.Role;
import com.manacommunity.api.model.RolePermission;
import com.manacommunity.api.repository.CommunityRepository;
import com.manacommunity.api.repository.RolePermissionRepository;
import com.manacommunity.api.security.AuditLogService;
import com.manacommunity.api.security.PasswordPolicy;
import com.manacommunity.api.service.RoleService;
import com.manacommunity.api.user.dto.AdminCreateUserRequest;
import com.manacommunity.api.user.model.AppUser;
import com.manacommunity.api.user.repository.AppUserRepository;
import com.manacommunity.api.user.service.AdminUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class AdminUserServiceImpl implements AdminUserService {

    /** Used when the admin leaves the password blank; the user resets it later. */
    private static final String DEFAULT_TEMP_PASSWORD = "TempPass123!";

    private final AppUserRepository userRepository;
    private final CommunityRepository communityRepository;
    private final RoleService roleService;
    private final RolePermissionRepository rolePermissionRepo;
    private final PasswordEncoder passwordEncoder;
    private final AuditLogService auditLog;

    @Override
    @Transactional
    public AppUser createUser(AdminCreateUserRequest req) {
        // 1. Resolve the target community (by id, else invite code).
        Community community = resolveCommunity(req);

        // 2. Duplicate email / phone check (both UNIQUE in the DB).
        String email = req.getEmail().trim();
        String phone = req.getPhone().trim();
        if (userRepository.existsByEmail(email)) {
            throw new DuplicateResourceException("User", "email", email);
        }
        if (userRepository.existsByPhone(phone)) {
            throw new DuplicateResourceException("User", "phone", phone);
        }

        String fullName = (req.getFirstName().trim() + " " + req.getLastName().trim()).trim();

        // 3. Password (temporary when blank) + strength policy (also rejects
        //    passwords derived from the user's own data).
        String rawPassword = (req.getPassword() == null || req.getPassword().isBlank())
                ? DEFAULT_TEMP_PASSWORD : req.getPassword();
        PasswordPolicy.validate(rawPassword, Arrays.asList(email, fullName, phone, community.getName()));

        // 4. Resolve role(s) — prefer req.getRoles() list, fall back to req.getRole() string.
        //    Each UI label is mapped to a backend role name.
        List<String> rawRoles = resolveRoleList(req);
        List<String> distinctRoles = rawRoles.stream().distinct().toList();

        // Primary role entity (used for role_id FK on app_user).
        String primaryRoleName = distinctRoles.get(0);
        Role primaryRoleEntity = roleService.findOrCreateRole(primaryRoleName, community.getId());
        if (primaryRoleEntity == null) {
            throw new InvalidInputException("Failed to resolve role: " + primaryRoleName);
        }

        // 5. Resolve ALL role entities and build the userRoles set that backs app_user_roles.
        java.util.Set<Role> resolvedRoles = new java.util.LinkedHashSet<>();
        for (String rName : distinctRoles) {
            Role rEntity = roleService.findOrCreateRole(rName, community.getId());
            if (rEntity != null) resolvedRoles.add(rEntity);
        }

        // Combined role string stored in app_user.role (e.g. "MEMBER, SPORTS_ADMIN").
        String combinedRoleStr = resolvedRoles.stream()
                .map(Role::getName)
                .collect(java.util.stream.Collectors.joining(", "));

        // 6. Build and persist the user with all captured fields.
        AppUser user = AppUser.builder()
                .fullName(fullName)
                .email(email)
                .phone(phone)
                .passwordHash(passwordEncoder.encode(rawPassword))
                .dateOfBirth(req.getDateOfBirth())
                .gender(mapGender(req.getGender()))
                .profilePicUrl(blankToNull(req.getProfilePic()))
                .employeeId(blankToNull(req.getEmployeeId()))
                .role(combinedRoleStr)
                .roleEntity(primaryRoleEntity)
                .userRoles(resolvedRoles)
                .kycStatus("VERIFIED") // admin-created accounts are pre-verified (matches self-registration)
                .community(community)
                .block(blankToNull(req.getBlock()))
                .tower(blankToNull(req.getTower()))
                .flatNo(blankToNull(req.getFlatNo()))
                .residentType(blankToNull(req.getResidentType()))
                .occupancyStatus(blankToNull(req.getOccupancyStatus()))
                .isActive(req.getIsActive() == null ? Boolean.TRUE : req.getIsActive())
                .notifyEmail(orDefault(req.getPrefEmail(), true))
                .notifySms(orDefault(req.getPrefSms(), false))
                .notifyWhatsapp(orDefault(req.getPrefWhatsapp(), true))
                .notifyPush(orDefault(req.getPrefPush(), true))
                .build();

        AppUser saved = userRepository.save(user);

        // 7. Copy permission templates for ALL assigned roles onto user-specific rows
        //    so the new account carries the full merged permission set.
        assignMultiRolePermissions(saved, distinctRoles, community.getId());

        auditLog.record(AuditLogService.Action.REGISTER, saved.getId(), saved.getEmail());
        return saved;
    }

    /**
     * Resolves the effective role list from the request.
     * Prefers {@code req.getRoles()} (multi-role list); falls back to {@code req.getRole()} string.
     * Each value is mapped from its UI label to a backend role name.
     * Always returns at least one element (defaults to ROLE_USER).
     */
    private List<String> resolveRoleList(AdminCreateUserRequest req) {
        if (req.getRoles() != null && !req.getRoles().isEmpty()) {
            List<String> mapped = req.getRoles().stream()
                    .filter(r -> r != null && !r.isBlank())
                    .map(this::mapRole)
                    .distinct()
                    .toList();
            if (!mapped.isEmpty()) return mapped;
        }
        // Fall back to single role string.
        return List.of(mapRole(req.getRole()));
    }

    private Community resolveCommunity(AdminCreateUserRequest req) {
        if (req.getCommunityId() != null) {
            return communityRepository.findById(req.getCommunityId())
                    .orElseThrow(() -> new ResourceNotFoundException("Community", req.getCommunityId()));
        }
        if (req.getInviteCode() != null && !req.getInviteCode().isBlank()) {
            return communityRepository.findByInviteCode(req.getInviteCode().trim())
                    .orElseThrow(() -> new InvalidInviteCodeException(req.getInviteCode()));
        }
        throw new InvalidInputException("A community is required (communityId or inviteCode).");
    }

    /**
     * Merges permission templates from every assigned role and saves them as
     * user-specific rows. Permission keys are de-duplicated across roles so the
     * unique constraint on (role_id, permission_key, user_id) is never violated.
     * Each resulting row carries the role name that first contributed that key.
     */
    private void assignMultiRolePermissions(AppUser user, List<String> roles, Long communityId) {
        rolePermissionRepo.deleteByUserId(user.getId());
        rolePermissionRepo.flush();

        // Track already-seen permission keys to guarantee uniqueness.
        java.util.Set<String> seen = new java.util.LinkedHashSet<>();
        List<RolePermission> allPermissions = new java.util.ArrayList<>();

        for (String roleName : roles) {
            Role rEntity = roleService.findOrCreateRole(roleName, communityId);
            if (rEntity == null || rEntity.getPermissions() == null) continue;

            rEntity.getPermissions().stream()
                    .filter(t -> t.getUser() == null)  // template rows only
                    .map(RolePermission::getPermissionKey)
                    .filter(pk -> pk != null && !pk.isBlank())
                    .filter(seen::add)                 // de-duplicate across roles
                    .forEach(pk -> allPermissions.add(RolePermission.builder()
                            .role(roleName)
                            .roleEntity(rEntity)
                            .permissionKey(pk)
                            .user(user)
                            .build()));
        }

        if (!allPermissions.isEmpty()) {
            rolePermissionRepo.saveAll(allPermissions);
        }
    }

    /** Maps a UI role (admin/committee/resident/security/vendor/staff) to a backend role. */
    private String mapRole(String uiRole) {
        if (uiRole == null || uiRole.isBlank()) {
            return ROLE_USER; // All new accounts default to USER until an admin assigns a higher role
        }
        String r = uiRole.trim();
        // "resident" is the standard member role in this system.
        if (r.equalsIgnoreCase("resident") || r.equalsIgnoreCase("member")) {
            return ROLE_MEMBER;
        }
        if (r.equalsIgnoreCase("user")) {
            return ROLE_USER;
        }
        return r.toUpperCase();
    }

    /** Maps a gender label from the form to the stored code (MALE/FEMALE/OTHER). */
    private String mapGender(String label) {
        if (label == null) return "OTHER";
        String g = label.trim().toLowerCase();
        if (g.startsWith("male")) return "MALE";
        if (g.startsWith("female")) return "FEMALE";
        return "OTHER";
    }

    private static String blankToNull(String s) {
        return (s == null || s.isBlank()) ? null : s.trim();
    }

    private static Boolean orDefault(Boolean value, boolean fallback) {
        return value == null ? fallback : value;
    }
}
