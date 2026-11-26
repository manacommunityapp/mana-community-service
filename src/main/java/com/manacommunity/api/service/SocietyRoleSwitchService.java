package com.manacommunity.api.service;

import com.manacommunity.api.model.Community;
import com.manacommunity.api.model.UserSocietyRole;
import com.manacommunity.api.repository.CommunityRepository;
import com.manacommunity.api.repository.UserSocietyRoleRepository;
import com.manacommunity.api.user.model.AppUser;
import com.manacommunity.api.user.repository.AppUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SocietyRoleSwitchService {

    private final UserSocietyRoleRepository societyRoleRepo;
    private final AppUserRepository appUserRepo;
    private final CommunityRepository communityRepo;
    private final RolePermissionService rolePermissionService;

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getUserSocieties(Long userId) {
        List<UserSocietyRole> roles = societyRoleRepo.findByUserIdAndActiveTrue(userId);
        return roles.stream().map(r -> {
            Community c = r.getCommunity();
            return Map.<String, Object>of(
                    "societyRoleId", r.getId(),
                    "communityId", c.getId(),
                    "communityName", c.getName(),
                    "communityType", c.getType() != null ? c.getType() : "",
                    "roleType", r.getRoleType(),
                    "flatNo", r.getFlatNo() != null ? r.getFlatNo() : "",
                    "isPrimary", r.getIsPrimary()
            );
        }).toList();
    }

    @Transactional
    public Map<String, Object> switchSociety(Long userId, Long communityId, String roleType) {
        UserSocietyRole targetRole = societyRoleRepo
                .findByUserIdAndCommunityIdAndRoleType(userId, communityId, roleType)
                .orElseThrow(() -> new IllegalArgumentException(
                        "No role " + roleType + " found for user " + userId + " in community " + communityId));

        if (!targetRole.getActive()) {
            throw new IllegalStateException("Role is deactivated");
        }

        AppUser user = appUserRepo.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

        Community community = communityRepo.findById(communityId)
                .orElseThrow(() -> new IllegalArgumentException("Community not found: " + communityId));
        user.setCommunity(community);
        user.setFlatNo(targetRole.getFlatNo());
        appUserRepo.save(user);

        List<UserSocietyRole> allRoles = societyRoleRepo.findByUserIdAndActiveTrue(userId);
        allRoles.forEach(r -> {
            r.setIsPrimary(r.getId().equals(targetRole.getId()));
            societyRoleRepo.save(r);
        });

        List<String> permissions = rolePermissionService.getEffectivePermissionsForUser(user);

        return Map.of(
                "communityId", communityId,
                "communityName", community.getName(),
                "roleType", roleType,
                "flatNo", targetRole.getFlatNo() != null ? targetRole.getFlatNo() : "",
                "permissions", permissions,
                "switched", true
        );
    }

    @Transactional
    public UserSocietyRole addSocietyRole(Long userId, Long communityId, String roleType,
                                            String flatNo) {
        societyRoleRepo.findByUserIdAndCommunityIdAndRoleType(userId, communityId, roleType)
                .ifPresent(existing -> {
                    throw new IllegalStateException("Role already exists: " + roleType);
                });

        AppUser user = appUserRepo.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        Community community = communityRepo.findById(communityId)
                .orElseThrow(() -> new IllegalArgumentException("Community not found"));

        boolean isFirst = societyRoleRepo.findByUserIdAndActiveTrue(userId).isEmpty();

        UserSocietyRole role = UserSocietyRole.builder()
                .user(user)
                .community(community)
                .roleType(roleType)
                .flatNo(flatNo)
                .isPrimary(isFirst)
                .active(true)
                .build();
        return societyRoleRepo.save(role);
    }

    @Transactional
    public void removeSocietyRole(Long userId, Long communityId, String roleType) {
        UserSocietyRole role = societyRoleRepo
                .findByUserIdAndCommunityIdAndRoleType(userId, communityId, roleType)
                .orElseThrow(() -> new IllegalArgumentException("Role not found"));
        role.setActive(false);
        societyRoleRepo.save(role);
    }

    @Transactional(readOnly = true)
    public Map<String, List<Map<String, Object>>> getCommunityMembers(Long communityId) {
        List<UserSocietyRole> roles = societyRoleRepo.findByCommunityIdAndActiveTrue(communityId);
        return Map.of("members", roles.stream().map(r -> Map.<String, Object>of(
                "userId", r.getUser().getId(),
                "userName", r.getUser().getFullName() != null ? r.getUser().getFullName() : "",
                "roleType", r.getRoleType(),
                "flatNo", r.getFlatNo() != null ? r.getFlatNo() : ""
        )).toList());
    }
}
