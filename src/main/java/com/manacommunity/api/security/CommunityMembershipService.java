package com.manacommunity.api.security;

import com.manacommunity.api.model.Community;
import com.manacommunity.api.repository.CommunityRepository;
import com.manacommunity.api.resident.model.FlatMembership;
import com.manacommunity.api.resident.repository.FlatMembershipRepository;
import com.manacommunity.api.user.model.AppUser;
import com.manacommunity.api.user.repository.AppUserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.manacommunity.api.constants.PermissionConstants.ROLE_SUPER_ADMIN;

/**
 * Authoritative service that resolves and verifies community and property
 * memberships for users. Client headers (e.g. X-Community-Id) must ALWAYS be
 * verified through this service against the authenticated user's records.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CommunityMembershipService {

    private final AppUserRepository userRepository;
    private final FlatMembershipRepository flatMembershipRepository;
    private final CommunityRepository communityRepository;

    /**
     * Returns the set of community IDs the given user is authorized to access.
     * Includes direct community association and active flat memberships.
     */
    @Transactional(readOnly = true)
    public Set<Long> getAuthorizedCommunityIds(AppUser user) {
        if (user == null) {
            return Collections.emptySet();
        }

        Set<Long> communityIds = new HashSet<>();

        // 1. Direct assigned community on AppUser
        if (user.getCommunity() != null && user.getCommunity().getId() != null) {
            communityIds.add(user.getCommunity().getId());
        }

        // 2. Flat memberships (active / not REVOKED)
        if (user.getId() != null) {
            List<FlatMembership> memberships = flatMembershipRepository.findByUserId(user.getId());
            for (FlatMembership fm : memberships) {
                if (!"REVOKED".equalsIgnoreCase(fm.getAppAccessStatus())
                        && fm.getFlat() != null
                        && fm.getFlat().getCommunity() != null
                        && fm.getFlat().getCommunity().getId() != null) {
                    communityIds.add(fm.getFlat().getCommunity().getId());
                }
            }
        }

        return Collections.unmodifiableSet(communityIds);
    }

    /**
     * Checks if the user is authorized for the given community ID.
     * Super admins are authorized across all communities.
     */
    @Transactional(readOnly = true)
    public boolean isUserAuthorizedForCommunity(AppUser user, Long communityId) {
        if (user == null || communityId == null) {
            return false;
        }

        if (user.hasRole(ROLE_SUPER_ADMIN)) {
            return true;
        }

        return getAuthorizedCommunityIds(user).contains(communityId);
    }

    /**
     * Convenience method to check community authorization by user ID.
     */
    @Transactional(readOnly = true)
    public boolean isUserAuthorizedForCommunity(Long userId, Long communityId) {
        if (userId == null || communityId == null) {
            return false;
        }

        return userRepository.findById(userId)
                .map(user -> isUserAuthorizedForCommunity(user, communityId))
                .orElse(false);
    }

    /**
     * Checks if the user has an active membership for the specified flat/property.
     */
    @Transactional(readOnly = true)
    public boolean isUserAuthorizedForProperty(AppUser user, Long flatId) {
        if (user == null || flatId == null) {
            return false;
        }

        if (user.hasRole(ROLE_SUPER_ADMIN)) {
            return true;
        }

        return flatMembershipRepository.findByUserIdAndFlatId(user.getId(), flatId)
                .filter(fm -> !"REVOKED".equalsIgnoreCase(fm.getAppAccessStatus()))
                .isPresent();
    }

    /**
     * Resolves the primary/default community ID for the user when none is explicitly requested.
     */
    @Transactional(readOnly = true)
    public Long resolveDefaultCommunityId(AppUser user) {
        if (user == null) {
            return null;
        }

        if (user.getCommunity() != null && user.getCommunity().getId() != null) {
            return user.getCommunity().getId();
        }

        if (user.getId() != null) {
            List<FlatMembership> memberships = flatMembershipRepository.findByUserId(user.getId());
            // Prefer primary resident membership
            for (FlatMembership fm : memberships) {
                if (Boolean.TRUE.equals(fm.getIsPrimaryResident())
                        && !"REVOKED".equalsIgnoreCase(fm.getAppAccessStatus())
                        && fm.getFlat() != null && fm.getFlat().getCommunity() != null) {
                    return fm.getFlat().getCommunity().getId();
                }
            }
            // Fallback to first non-revoked membership
            for (FlatMembership fm : memberships) {
                if (!"REVOKED".equalsIgnoreCase(fm.getAppAccessStatus())
                        && fm.getFlat() != null && fm.getFlat().getCommunity() != null) {
                    return fm.getFlat().getCommunity().getId();
                }
            }
        }

        return null;
    }

    /**
     * Resolves the organization ID for the given community and user.
     */
    @Transactional(readOnly = true)
    public Long resolveOrganizationId(Long communityId, AppUser user) {
        if (communityId != null) {
            Community community = communityRepository.findById(communityId).orElse(null);
            if (community != null && community.getOrganizationId() != null) {
                return community.getOrganizationId();
            }
        }

        if (user != null && user.getOrganizationId() != null) {
            return user.getOrganizationId();
        }

        return com.manacommunity.api.config.tenant.TenantContext.DEFAULT_ORG_ID;
    }
}
