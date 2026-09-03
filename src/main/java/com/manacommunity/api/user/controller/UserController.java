package com.manacommunity.api.user.controller;

import com.manacommunity.api.model.Role;
import com.manacommunity.api.user.repository.AppUserRepository;
import static com.manacommunity.api.constants.PermissionConstants.*;
import com.manacommunity.api.dto.PagedResponse;
import com.manacommunity.api.user.model.AppUser;
import com.manacommunity.api.user.dto.ChangePasswordRequest;
import com.manacommunity.api.user.dto.UpdateUserRequest;
import com.manacommunity.api.user.dto.UserResponse;
import com.manacommunity.api.user.security.UserPrincipal;
import com.manacommunity.api.user.service.AdminUserService;
import com.manacommunity.api.user.service.AuthService;
import com.manacommunity.api.user.service.LoggedInUserService;
import com.manacommunity.api.user.service.MenuRolePermissionService;
import com.manacommunity.api.repository.RolePermissionRepository;
import com.manacommunity.api.service.RoleService;
import com.manacommunity.api.service.CommunityModuleService;
import com.manacommunity.api.exception.DuplicateResourceException;
import com.manacommunity.api.exception.InvalidInputException;
import com.manacommunity.api.exception.ResourceNotFoundException;
import com.manacommunity.api.exception.UnauthorizedActionException;
import com.manacommunity.api.security.PasswordPolicy;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final LoggedInUserService loggedInUserService;
    private final RolePermissionRepository rolePermissionRepo;
    private final RoleService roleService;
    private final CommunityModuleService communityModuleService;
    private final MenuRolePermissionService menuRolePermissionService;
    private final AppUserRepository appUserRepo;
    private final AdminUserService adminUserService;
    private final PasswordEncoder passwordEncoder;
    private final AuthService authService;
    private final com.manacommunity.api.service.RolePermissionService rolePermissionService;

    private java.util.List<String> getRolesList(String roleStr) {
        if (roleStr == null || roleStr.isBlank()) {
            return java.util.Collections.emptyList();
        }
        return java.util.Arrays.stream(roleStr.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .distinct()
                .toList();
    }

    private java.util.List<String> getPermissionsForUser(AppUser user) {
        if (user == null || user.getId() == null) return java.util.Collections.emptyList();
        return rolePermissionService.getEffectivePermissionsForUser(user);
    }

    private java.util.List<com.manacommunity.api.user.dto.MenuRolePermissionResponse> getMenuPermissions(AppUser user) {
        if (user.hasRole(ROLE_SUPER_ADMIN)) {
            return java.util.Collections.emptyList();
        }
        Role roleEntity = user.getRoleEntity();
        if (roleEntity == null) {
            return java.util.Collections.emptyList();
        }
        return menuRolePermissionService.getViewableMenus(roleEntity.getId());
    }

    private java.util.List<String> getEnabledModules(AppUser user) {
        if (user.hasRole(ROLE_SUPER_ADMIN)) {
            return com.manacommunity.api.constants.ModuleConstants.ALL_MODULES.stream()
                    .map(com.manacommunity.api.constants.ModuleConstants.ModuleDef::key)
                    .toList();
        }
        if (user.getCommunity() == null) {
            return java.util.Collections.emptyList();
        }
        return communityModuleService.getEnabledModuleKeys(user.getCommunity().getId());
    }

    @GetMapping("/me")
    public ResponseEntity<UserResponse> getLoggedInUserDetails(@AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(toUserResponse(user));
    }

    @GetMapping("/search")
    public ResponseEntity<java.util.List<UserResponse>> searchUsers(
            @RequestParam(required = false) Long communityId,
            @RequestParam String query,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser loggedInUser = loggedInUserService.resolve(principal);
        Long targetCommunityId = communityId;
        if (!loggedInUser.hasRole(ROLE_SUPER_ADMIN)) {
            targetCommunityId = loggedInUser.getCommunity() != null ? loggedInUser.getCommunity().getId() : null;
        }
        if (targetCommunityId == null) {
            return ResponseEntity.ok(java.util.Collections.emptyList());
        }
        final Long finalCommId = targetCommunityId;
        return ResponseEntity.ok(appUserRepo.findByCommunityIdAndFullNameContainingIgnoreCase(finalCommId, query)
                .stream().map(this::toUserResponse).toList());
    }

    @GetMapping("/community/{communityId}")
    public ResponseEntity<java.util.List<UserResponse>> getCommunityUsers(
            @PathVariable Long communityId,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser loggedInUser = loggedInUserService.resolve(principal);
        Long targetCommunityId = communityId;
        if (!loggedInUser.hasRole(ROLE_SUPER_ADMIN)) {
            targetCommunityId = loggedInUser.getCommunity() != null ? loggedInUser.getCommunity().getId() : null;
            if (targetCommunityId == null || !targetCommunityId.equals(communityId)) {
                throw new com.manacommunity.api.exception.UnauthorizedActionException(
                        "You can only view users from your own community.");
            }
        }
        final Long finalCommId = targetCommunityId;
        return ResponseEntity.ok(appUserRepo.findByCommunityId(finalCommId)
                .stream().map(this::toUserResponse).toList());
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN','COMMUNITY_ADMIN')")
    public ResponseEntity<PagedResponse<UserResponse>> getAllUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
            @RequestParam(required = false) Long communityId,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String kycStatus,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser loggedInUser = loggedInUserService.resolve(principal);
        boolean isSuperAdmin = loggedInUser.hasRole(ROLE_SUPER_ADMIN);
        Long targetCommunityId = isSuperAdmin ? communityId : (loggedInUser.getCommunity() != null ? loggedInUser.getCommunity().getId() : null);

        if (!isSuperAdmin && targetCommunityId == null) {
            return ResponseEntity.ok(PagedResponse.empty());
        }

        int safeSize = Math.min(Math.max(size, 1), 500);
        PageRequest pageable = PageRequest.of(Math.max(page, 0), safeSize, Sort.by("fullName").ascending());

        org.springframework.data.jpa.domain.Specification<AppUser> spec = (root, query, cb) -> {
            java.util.List<jakarta.persistence.criteria.Predicate> predicates = new java.util.ArrayList<>();

            if (targetCommunityId != null) {
                predicates.add(cb.equal(root.get("community").get("id"), targetCommunityId));
            }

            if (kycStatus != null && !kycStatus.trim().isEmpty() && !"ALL".equalsIgnoreCase(kycStatus.trim())) {
                predicates.add(cb.equal(cb.upper(root.get("kycStatus")), kycStatus.trim().toUpperCase()));
            }

            if (status != null && !status.trim().isEmpty() && !"ALL".equalsIgnoreCase(status.trim())) {
                if ("ACTIVE".equalsIgnoreCase(status.trim())) {
                    predicates.add(cb.isTrue(root.get("isActive")));
                } else if ("INACTIVE".equalsIgnoreCase(status.trim())) {
                    predicates.add(cb.isFalse(root.get("isActive")));
                }
            }
            if (search != null && !search.trim().isEmpty()) {
                String pattern = "%" + search.trim().toLowerCase() + "%";
                java.util.List<jakarta.persistence.criteria.Predicate> searchPredicates = new java.util.ArrayList<>();
                searchPredicates.add(cb.like(cb.lower(root.get("fullName")), pattern));
                searchPredicates.add(cb.like(cb.lower(root.get("email")), pattern));
                searchPredicates.add(cb.like(cb.lower(root.get("phone")), pattern));
                searchPredicates.add(cb.like(cb.lower(root.get("role")), pattern));
                searchPredicates.add(cb.like(cb.lower(root.get("flatNo")), pattern));
                searchPredicates.add(cb.like(cb.lower(root.get("block")), pattern));
                searchPredicates.add(cb.like(cb.lower(root.get("tower")), pattern));
                searchPredicates.add(cb.like(cb.lower(root.get("employeeId")), pattern));
                predicates.add(cb.or(searchPredicates.toArray(new jakarta.persistence.criteria.Predicate[0])));
            }

            return cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        };

        Page<AppUser> userPage = appUserRepo.findAll(spec, pageable);

        return ResponseEntity.ok(PagedResponse.from(userPage, this::toUserResponse));
    }

    @GetMapping("/stats")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN','COMMUNITY_ADMIN')")
    public ResponseEntity<com.manacommunity.api.user.dto.UserStatsResponse> getUserStats(
            @RequestParam(required = false) Long communityId,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser loggedInUser = loggedInUserService.resolve(principal);
        boolean isSuperAdmin = loggedInUser.hasRole(ROLE_SUPER_ADMIN);
        Long targetCommunityId = isSuperAdmin ? communityId : (loggedInUser.getCommunity() != null ? loggedInUser.getCommunity().getId() : null);

        long totalUsers;
        long activeUsers;
        long pendingKyc;
        long approvedKyc;
        long rejectedKyc;
        java.util.Map<String, Long> roleBreakdown = new java.util.HashMap<>();

        if (targetCommunityId != null) {
            totalUsers = appUserRepo.countByCommunityId(targetCommunityId);
            activeUsers = appUserRepo.countByCommunityIdAndIsActiveTrue(targetCommunityId);
            pendingKyc = appUserRepo.countByCommunityIdAndKycStatus(targetCommunityId, "PENDING");
            approvedKyc = appUserRepo.countByCommunityIdAndKycStatus(targetCommunityId, "APPROVED")
                    + appUserRepo.countByCommunityIdAndKycStatus(targetCommunityId, "VERIFIED");
            rejectedKyc = appUserRepo.countByCommunityIdAndKycStatus(targetCommunityId, "REJECTED");

            java.util.List<Object[]> roleRows = appUserRepo.countByRoleGroupedForCommunity(targetCommunityId);
            for (Object[] row : roleRows) {
                if (row[0] != null) {
                    roleBreakdown.put(row[0].toString(), ((Number) row[1]).longValue());
                }
            }
        } else {
            totalUsers = appUserRepo.count();
            activeUsers = appUserRepo.countByIsActiveTrue();
            pendingKyc = appUserRepo.countByKycStatus("PENDING");
            approvedKyc = appUserRepo.countByKycStatus("APPROVED") + appUserRepo.countByKycStatus("VERIFIED");
            rejectedKyc = appUserRepo.countByKycStatus("REJECTED");

            java.util.List<Object[]> roleRows = appUserRepo.countByRoleGrouped();
            for (Object[] row : roleRows) {
                if (row[0] != null) {
                    roleBreakdown.put(row[0].toString(), ((Number) row[1]).longValue());
                }
            }
        }

        return ResponseEntity.ok(com.manacommunity.api.user.dto.UserStatsResponse.builder()
                .totalUsers(totalUsers)
                .activeUsers(activeUsers)
                .pendingKyc(pendingKyc)
                .approvedKyc(approvedKyc)
                .rejectedKyc(rejectedKyc)
                .roleBreakdown(roleBreakdown)
                .build());
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN','COMMUNITY_ADMIN')")
    public ResponseEntity<UserResponse> createUser(
            @jakarta.validation.Valid @RequestBody com.manacommunity.api.user.dto.AdminCreateUserRequest req,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser loggedInUser = loggedInUserService.resolve(principal);
        // Non-super-admins may only create users within their own community.
        if (!loggedInUser.hasRole(ROLE_SUPER_ADMIN) && loggedInUser.getCommunity() != null) {
            req.setCommunityId(loggedInUser.getCommunity().getId());
            req.setInviteCode(null);
        }

        AppUser saved = adminUserService.createUser(req);
        return ResponseEntity.status(org.springframework.http.HttpStatus.CREATED).body(toUserResponse(saved));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN','COMMUNITY_ADMIN')")
    public ResponseEntity<UserResponse> getUserById(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = appUserRepo.findById(id)
                .orElseThrow(() -> new com.manacommunity.api.exception.ResourceNotFoundException("User", id));
        return ResponseEntity.ok(toUserResponse(user));
    }

    private UserResponse toUserResponse(AppUser u) {
        return UserResponse.builder()
                .id(u.getId())
                .fullName(u.getFullName())
                .email(u.getEmail())
                .phone(u.getPhone())
                .role(u.getRole())
                .roles(getRolesList(u.getRole()))
                .kycStatus(u.getKycStatus())
                .profilePicUrl(u.getProfilePicUrl())
                .gender(u.getGender())
                .dateOfBirth(u.getDateOfBirth())
                .flatNo(u.getFlatNo())
                .block(u.getBlock())
                .tower(u.getTower())
                .employeeId(u.getEmployeeId())
                .govtIdType(u.getGovtIdType())
                .govtIdNumber(u.getGovtIdNumber())
                .communityId(u.getCommunity() != null ? u.getCommunity().getId() : null)
                .roleId(u.getRoleEntity() != null ? u.getRoleEntity().getId() : null)
                .isActive(u.getIsActive())
                .occupancyStatus(u.getOccupancyStatus())
                .residentType(u.getResidentType())
                .userType(u.getOccupancyStatus() != null ? u.getOccupancyStatus() : "Owner")
                .permissions(getPermissionsForUser(u))
                .enabledModules(getEnabledModules(u))
                .menuPermissions(getMenuPermissions(u))
                .roleChangedAt(u.getRoleChangedAt())
                .roleChangedBy(u.getRoleChangedBy())
                .createdAt(u.getCreatedAt())
                .updatedAt(u.getUpdatedAt())
                .build();
    }

    /**
     * Changes the password for the currently authenticated user via /api/users/change-password.
     */
    @PostMapping("/change-password")
    public ResponseEntity<Map<String, Object>> changePassword(
            @AuthenticationPrincipal UserPrincipal principal,
            @jakarta.validation.Valid @RequestBody ChangePasswordRequest request) {
        if (principal == null || principal.getId() == null) {
            throw new UnauthorizedActionException("Authentication is required to change password.");
        }
        authService.changePassword(principal.getId(), request);
        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Password changed successfully."
        ));
    }

    @PutMapping("/change-password")
    public ResponseEntity<Map<String, Object>> changePasswordPut(
            @AuthenticationPrincipal UserPrincipal principal,
            @jakarta.validation.Valid @RequestBody ChangePasswordRequest request) {
        return changePassword(principal, request);
    }

    /**
     * Updates an existing user by ID (used for self profile update fallback or admin user update).
     */
    @PutMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    @org.springframework.transaction.annotation.Transactional
    public ResponseEntity<UserResponse> updateUser(
            @PathVariable Long id,
            @RequestBody UpdateUserRequest req,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser loggedInUser = loggedInUserService.resolve(principal);
        boolean isSelf = loggedInUser.getId() != null && loggedInUser.getId().equals(id);
        boolean isAdmin = loggedInUser.hasRole(ROLE_SUPER_ADMIN)
                || loggedInUser.hasRole(ROLE_COMMUNITY_ADMIN)
                || loggedInUser.hasRole(ROLE_ADMIN);

        if (!isSelf && !isAdmin) {
            throw new UnauthorizedActionException("You are not authorized to update this user.");
        }

        AppUser user = appUserRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", id));

        // If admin (non-super-admin), verify community scoping
        if (isAdmin && !loggedInUser.hasRole(ROLE_SUPER_ADMIN) && !isSelf) {
            Long adminCommunityId = loggedInUser.getCommunity() != null ? loggedInUser.getCommunity().getId() : null;
            Long userCommunityId = user.getCommunity() != null ? user.getCommunity().getId() : null;
            if (adminCommunityId == null || !adminCommunityId.equals(userCommunityId)) {
                throw new UnauthorizedActionException("You can only manage users within your own community.");
            }
        }

        // 1. Update personal details
        if (req.getFullName() != null && !req.getFullName().isBlank()) {
            user.setFullName(req.getFullName().trim());
        } else if (req.getFirstName() != null || req.getLastName() != null) {
            String first = req.getFirstName() != null ? req.getFirstName().trim() : "";
            String last = req.getLastName() != null ? req.getLastName().trim() : "";
            String full = (first + " " + last).trim();
            if (!full.isEmpty()) {
                user.setFullName(full);
            }
        }

        if (req.getEmail() != null && !req.getEmail().isBlank() && !req.getEmail().equalsIgnoreCase(user.getEmail())) {
            String newEmail = req.getEmail().trim().toLowerCase();
            if (appUserRepo.existsByEmail(newEmail)) {
                throw new DuplicateResourceException("User", "email", newEmail);
            }
            user.setEmail(newEmail);
        }

        if (req.getPhone() != null && !req.getPhone().isBlank() && !req.getPhone().equals(user.getPhone())) {
            String newPhone = req.getPhone().trim();
            if (appUserRepo.existsByPhone(newPhone)) {
                throw new DuplicateResourceException("User", "phone", newPhone);
            }
            user.setPhone(newPhone);
        }

        if (req.getDateOfBirth() != null) user.setDateOfBirth(req.getDateOfBirth());
        if (req.getGender() != null) user.setGender(req.getGender());
        if (req.getFlatNo() != null) user.setFlatNo(req.getFlatNo());
        if (req.getBlock() != null) user.setBlock(req.getBlock());
        if (req.getProfilePicUrl() != null && !req.getProfilePicUrl().isBlank()) {
            user.setProfilePicUrl(req.getProfilePicUrl());
        } else if (req.getProfilePic() != null && !req.getProfilePic().isBlank()) {
            user.setProfilePicUrl(req.getProfilePic());
        }

        if (req.getIsActive() != null && isAdmin) {
            user.setIsActive(req.getIsActive());
        }

        // 2. Optional password update
        if (req.getPassword() != null && !req.getPassword().isBlank()) {
            if (isSelf && !isAdmin) {
                if (req.getCurrentPassword() == null || req.getCurrentPassword().isBlank()) {
                    throw new InvalidInputException("Current password is required to change password.");
                }
                if (!passwordEncoder.matches(req.getCurrentPassword(), user.getPasswordHash())) {
                    throw new InvalidInputException("Current password does not match.");
                }
            }
            PasswordPolicy.validate(req.getPassword(), Arrays.asList(
                    user.getEmail(),
                    user.getFullName(),
                    user.getPhone(),
                    user.getCommunity() != null ? user.getCommunity().getName() : null
            ));
            user.setPasswordHash(passwordEncoder.encode(req.getPassword()));
            user.setFailedLoginAttempts(0);
            user.setLockedUntil(null);
        }

        if (req.getTower() != null) user.setTower(req.getTower());
        if (req.getResidentType() != null) user.setResidentType(req.getResidentType());
        if (req.getOccupancyStatus() != null) user.setOccupancyStatus(req.getOccupancyStatus());
        if (req.getEmployeeId() != null) user.setEmployeeId(req.getEmployeeId());
        if (req.getGovtIdType() != null) user.setGovtIdType(req.getGovtIdType());
        if (req.getGovtIdNumber() != null) user.setGovtIdNumber(req.getGovtIdNumber());

        AppUser saved = appUserRepo.save(user);

        return ResponseEntity.ok(toUserResponse(saved));
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN','COMMUNITY_ADMIN')")
    public ResponseEntity<Void> toggleUserStatus(@PathVariable Long id) {
        AppUser user = appUserRepo.findById(id)
                .orElseThrow(() -> new com.manacommunity.api.exception.ResourceNotFoundException("User", id));
        user.setIsActive(!user.getIsActive());
        appUserRepo.save(user);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/{id}/role")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN','COMMUNITY_ADMIN')")
    @org.springframework.transaction.annotation.Transactional
    public ResponseEntity<UserResponse> updateUserRole(
            @PathVariable Long id,
            @RequestBody com.manacommunity.api.user.dto.RoleAssignmentRequest req,
            @AuthenticationPrincipal UserPrincipal principal) {

        AppUser user = appUserRepo.findById(id)
                .orElseThrow(() -> new com.manacommunity.api.exception.ResourceNotFoundException("User", id));

        java.util.List<String> targetRoles = new java.util.ArrayList<>();
        if (req.getRoles() != null && !req.getRoles().isEmpty()) {
            req.getRoles().stream()
                    .map(s -> s == null ? "" : s.trim().toUpperCase())
                    .filter(s -> !s.isEmpty())
                    .forEach(targetRoles::add);
        } else if (req.getRole() != null && !req.getRole().isBlank()) {
            java.util.Arrays.stream(req.getRole().split(","))
                    .map(s -> s.trim().toUpperCase())
                    .filter(s -> !s.isEmpty())
                    .forEach(targetRoles::add);
        }

        if (targetRoles.isEmpty()) {
            throw new com.manacommunity.api.exception.InvalidInputException("At least one role is required");
        }

        java.util.List<String> distinctRoles = new java.util.ArrayList<>(targetRoles.stream().distinct().toList());
        // Every user always retains the USER base role
        if (distinctRoles.stream().noneMatch(r -> r.equalsIgnoreCase("USER"))) {
            distinctRoles.add("USER");
        }

        // Non-super-admins may only change roles within their own community.
        AppUser admin = loggedInUserService.resolve(principal);
        if (!admin.hasRole(ROLE_SUPER_ADMIN)) {
            Long adminCommunityId = admin.getCommunity() != null ? admin.getCommunity().getId() : null;
            Long userCommunityId  = user.getCommunity() != null ? user.getCommunity().getId() : null;
            if (adminCommunityId == null || !adminCommunityId.equals(userCommunityId)) {
                throw new com.manacommunity.api.exception.UnauthorizedActionException(
                        "You can only manage roles within your own community.");
            }
        }

        // Resolve ALL role entities and build the authoritative userRoles set.
        Long communityId = user.getCommunity() != null ? user.getCommunity().getId() : null;
        java.util.Set<com.manacommunity.api.model.Role> resolvedRoles = new java.util.LinkedHashSet<>();
        for (String rName : distinctRoles) {
            com.manacommunity.api.model.Role rEntity = roleService.findOrCreateRole(rName, communityId);
            if (rEntity != null) resolvedRoles.add(rEntity);
        }

        // Derive the comma-string from the set (canonical order = sorted names).
        String normRole = resolvedRoles.stream()
                .map(com.manacommunity.api.model.Role::getName)
                .sorted()
                .collect(java.util.stream.Collectors.joining(", "));
        user.setRole(normRole);
        user.setUserRoles(resolvedRoles);

        // Primary roleEntity FK (backward compat — points to the first resolved role).
        com.manacommunity.api.model.Role roleEntity = resolvedRoles.isEmpty()
                ? roleService.findOrCreateRole(distinctRoles.get(0), communityId)
                : resolvedRoles.iterator().next();
        if (roleEntity == null) {
            throw new com.manacommunity.api.exception.InvalidInputException("Failed to resolve role: " + distinctRoles.get(0));
        }
        user.setRoleEntity(roleEntity);

        // Track who changed the role and when
        user.setRoleChangedAt(java.time.LocalDateTime.now());
        user.setRoleChangedBy(admin.getId());

        appUserRepo.save(user);

        // Delete old user-specific permissions and replace with combined templates of all assigned roles.
        // A LinkedHashSet tracks seen keys cross-role to prevent unique-constraint violations.
        rolePermissionRepo.deleteByUserId(user.getId());
        rolePermissionRepo.flush();

        java.util.Set<String> seenPermissions = new java.util.LinkedHashSet<>();
        java.util.List<com.manacommunity.api.model.RolePermission> combinedUserPermissions = new java.util.ArrayList<>();
        for (String rName : distinctRoles) {
            com.manacommunity.api.model.Role rEntity = roleService.findOrCreateRole(rName, communityId);
            java.util.Set<com.manacommunity.api.model.RolePermission> templates =
                    (rEntity != null && rEntity.getPermissions() != null) ? rEntity.getPermissions() : java.util.Collections.emptySet();

            templates.stream()
                    .filter(t -> t.getUser() == null)
                    .map(com.manacommunity.api.model.RolePermission::getPermissionKey)
                    .filter(pk -> pk != null && !pk.trim().isEmpty())
                    .filter(seenPermissions::add)  // de-duplicate across all roles
                    .forEach(pk -> combinedUserPermissions.add(com.manacommunity.api.model.RolePermission.builder()
                            .role(rName)
                            .roleEntity(rEntity)
                            .permissionKey(pk)
                            .user(user)
                            .build()));
        }

        if (!combinedUserPermissions.isEmpty()) {
            rolePermissionRepo.saveAll(combinedUserPermissions);
        }

        return ResponseEntity.ok(toUserResponse(user));
    }

    @PutMapping("/{id}/kyc")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN','COMMUNITY_ADMIN')")
    public ResponseEntity<Void> updateUserKycStatus(@PathVariable Long id, @RequestBody java.util.Map<String, String> body) {
        AppUser user = appUserRepo.findById(id)
                .orElseThrow(() -> new com.manacommunity.api.exception.ResourceNotFoundException("User", id));
        String status = body.get("status");
        if (status == null || (!status.equals("VERIFIED") && !status.equals("REJECTED") && !status.equals("PENDING"))) {
            throw new com.manacommunity.api.exception.InvalidInputException("Valid status (PENDING, VERIFIED, REJECTED) is required");
        }
        user.setKycStatus(status);
        appUserRepo.save(user);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/kyc/stats")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN','COMMUNITY_ADMIN')")
    public ResponseEntity<com.manacommunity.api.user.dto.KycStatsResponse> getKycStats(
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser loggedInUser = loggedInUserService.resolve(principal);
        boolean isSuperAdmin = loggedInUser.hasRole(ROLE_SUPER_ADMIN);

        long total;
        long pending;
        long approved;
        long rejected;

        if (isSuperAdmin) {
            total = appUserRepo.count();
            approved = appUserRepo.countByKycStatus("VERIFIED");
            rejected = appUserRepo.countByKycStatus("REJECTED");
            pending = Math.max(0, total - approved - rejected);
        } else {
            Long communityId = loggedInUser.getCommunity() != null ? loggedInUser.getCommunity().getId() : null;
            if (communityId == null) {
                return ResponseEntity.ok(com.manacommunity.api.user.dto.KycStatsResponse.builder().build());
            }
            total = appUserRepo.countByCommunityId(communityId);
            approved = appUserRepo.countByCommunityIdAndKycStatus(communityId, "VERIFIED");
            rejected = appUserRepo.countByCommunityIdAndKycStatus(communityId, "REJECTED");
            pending = Math.max(0, total - approved - rejected);
        }

        return ResponseEntity.ok(com.manacommunity.api.user.dto.KycStatsResponse.builder()
                .total(total)
                .pending(pending)
                .approved(approved)
                .rejected(rejected)
                .build());
    }

    /**
     * GET /api/users/{id}/roles
     * Returns the structured list of roles currently assigned to a user,
     * sourced from the {@code app_user_roles} join table (authoritative).
     */
    @GetMapping("/{id}/roles")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN','COMMUNITY_ADMIN')")
    public ResponseEntity<java.util.List<java.util.Map<String, Object>>> getUserRoles(@PathVariable Long id) {
        AppUser user = appUserRepo.findById(id)
                .orElseThrow(() -> new com.manacommunity.api.exception.ResourceNotFoundException("User", id));

        java.util.List<java.util.Map<String, Object>> roleList = user.getUserRoles().stream()
                .filter(r -> r.getName() != null && !isRestrictedRoleName(r.getName()))
                .map(r -> java.util.Map.<String, Object>of(
                        "id",          r.getId(),
                        "name",        r.getName(),
                        "communityId", r.getCommunityId() != null ? r.getCommunityId() : java.util.Optional.empty()
                ))
                .toList();

        return ResponseEntity.ok(roleList);
    }

    private boolean isRestrictedRoleName(String roleName) {
        if (roleName == null) return false;
        String upper = roleName.trim().toUpperCase();
        return upper.equals("SUPER_ADMIN") || upper.equals("SUPERADMIN") || upper.equals("SUPER_ADMINISTRATOR")
                || upper.equals("COMMUNITY_ADMIN") || upper.equals("COMMUNITYADMIN") || upper.equals("COMMUNITY_ADMINISTRATOR") || upper.equals("COMMUNITY ADMIN");
    }

    /**
     * GET /api/users/{id}/permissions
     * Returns the effective (merged) permission keys for a user.
     * User-specific override rows win; otherwise the union of all role templates is returned.
     */
    @GetMapping("/{id}/permissions")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN','COMMUNITY_ADMIN')")
    public ResponseEntity<java.util.List<String>> getUserPermissions(@PathVariable Long id) {
        AppUser user = appUserRepo.findById(id)
                .orElseThrow(() -> new com.manacommunity.api.exception.ResourceNotFoundException("User", id));
        return ResponseEntity.ok(getPermissionsForUser(user));
    }
}
