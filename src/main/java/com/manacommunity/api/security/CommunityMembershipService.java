package com.manacommunity.api.security;

import com.manacommunity.api.marketplace.entity.MarketSellerProfile;
import com.manacommunity.api.marketplace.repository.MarketSellerRepository;
import com.manacommunity.api.model.Community;
import com.manacommunity.api.repository.CommunityRepository;
import com.manacommunity.api.resident.model.FlatMembership;
import com.manacommunity.api.resident.repository.FlatMembershipRepository;
import com.manacommunity.api.serviceplatform.entity.ServiceProvider;
import com.manacommunity.api.serviceplatform.entity.enums.VerificationStatus;
import com.manacommunity.api.serviceplatform.repository.ServiceProviderRepository;
import com.manacommunity.api.user.model.AppUser;
import com.manacommunity.api.user.repository.AppUserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import static com.manacommunity.api.constants.PermissionConstants.ROLE_SUPER_ADMIN;

/**
 * Authoritative service that resolves and verifies community and property
 * memberships for users across residential flats, service providers, and
 * marketplace seller profiles.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CommunityMembershipService {

    private final AppUserRepository userRepository;
    private final FlatMembershipRepository flatMembershipRepository;
    private final CommunityRepository communityRepository;

    @Autowired(required = false)
    private ServiceProviderRepository serviceProviderRepository;

    @Autowired(required = false)
    private MarketSellerRepository marketSellerRepository;

    public void setServiceProviderRepository(ServiceProviderRepository serviceProviderRepository) {
        this.serviceProviderRepository = serviceProviderRepository;
    }

    public void setMarketSellerRepository(MarketSellerRepository marketSellerRepository) {
        this.marketSellerRepository = marketSellerRepository;
    }

    public record CommunityRoleContext(
            Long communityId,
            String communityName,
            List<String> roles,
            List<String> flatNumbers,
            boolean isDefault
    ) {}

    /**
     * Returns the set of community IDs the given user is authorized to access:
     * direct AppUser community, active FlatMemberships, active ServiceProvider links,
     * and active MarketSellerProfile links.
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

        if (user.getId() != null) {
            // 2. Flat memberships (active / not REVOKED)
            List<FlatMembership> memberships = flatMembershipRepository.findByUserId(user.getId());
            if (memberships != null) {
                for (FlatMembership fm : memberships) {
                    if (!"REVOKED".equalsIgnoreCase(fm.getAppAccessStatus())
                            && fm.getFlat() != null
                            && fm.getFlat().getCommunity() != null
                            && fm.getFlat().getCommunity().getId() != null) {
                        communityIds.add(fm.getFlat().getCommunity().getId());
                    }
                }
            }

            // 3. Service providers operating in a community (not REJECTED)
            if (serviceProviderRepository != null) {
                List<ServiceProvider> providers = serviceProviderRepository.findAllByUserId(user.getId());
                if (providers != null) {
                    for (ServiceProvider sp : providers) {
                        if (sp.getVerificationStatus() != VerificationStatus.REJECTED
                                && sp.getCommunity() != null
                                && sp.getCommunity().getId() != null) {
                            communityIds.add(sp.getCommunity().getId());
                        }
                    }
                }
            }

            // 4. Marketplace sellers operating in a community
            if (marketSellerRepository != null) {
                List<MarketSellerProfile> sellers = marketSellerRepository.findByUserIdAndIsActiveTrue(user.getId());
                if (sellers != null) {
                    for (MarketSellerProfile seller : sellers) {
                        if (seller.getCommunity() != null && seller.getCommunity().getId() != null) {
                            communityIds.add(seller.getCommunity().getId());
                        }
                    }
                }
            }
        }

        return Collections.unmodifiableSet(communityIds);
    }

    /**
     * Resolves distinct roles specifically scoped to {@code targetCommunityId} so an Owner
     * in Community A does not leak Owner rights into Community B (where they are a Tenant)
     * or Community C (where they are a Vendor).
     */
    @Transactional(readOnly = true)
    public List<String> resolveRolesForCommunity(AppUser user, Long targetCommunityId) {
        if (user == null || targetCommunityId == null) {
            return Collections.emptyList();
        }
        Set<String> scoped = new LinkedHashSet<>();
        Long homeCommunityId = user.getCommunity() != null ? user.getCommunity().getId() : null;

        if (user.hasRole(ROLE_SUPER_ADMIN)) {
            scoped.add(ROLE_SUPER_ADMIN);
        }

        if (homeCommunityId != null && homeCommunityId.equals(targetCommunityId) && user.getRole() != null) {
            for (String rawRole : user.getRole().split(",")) {
                String normalized = rawRole.trim().toUpperCase(Locale.ROOT);
                if (!normalized.isEmpty()) {
                    scoped.add(normalized);
                }
            }
        }

        if (user.getId() != null) {
            List<FlatMembership> memberships = flatMembershipRepository.findByUserId(user.getId());
            if (memberships != null) {
                for (FlatMembership fm : memberships) {
                    if (!"REVOKED".equalsIgnoreCase(fm.getAppAccessStatus())
                            && fm.getFlat() != null
                            && fm.getFlat().getCommunity() != null
                            && targetCommunityId.equals(fm.getFlat().getCommunity().getId())) {
                        if (fm.getResidentType() != null && !fm.getResidentType().isBlank()) {
                            scoped.add(fm.getResidentType().trim().toUpperCase(Locale.ROOT));
                        }
                        scoped.add("RESIDENT");
                    }
                }
            }

            if (serviceProviderRepository != null) {
                List<ServiceProvider> providers = serviceProviderRepository.findAllByUserId(user.getId());
                if (providers != null) {
                    boolean hasProvider = providers.stream().anyMatch(sp ->
                            sp.getVerificationStatus() != VerificationStatus.REJECTED
                                    && sp.getCommunity() != null
                                    && targetCommunityId.equals(sp.getCommunity().getId()));
                    if (hasProvider) {
                        scoped.add("SERVICE_PROVIDER");
                        scoped.add("VENDOR");
                    }
                }
            }

            if (marketSellerRepository != null) {
                List<MarketSellerProfile> sellers = marketSellerRepository.findByUserIdAndIsActiveTrue(user.getId());
                if (sellers != null) {
                    boolean hasSeller = sellers.stream().anyMatch(s ->
                            s.getCommunity() != null && targetCommunityId.equals(s.getCommunity().getId()));
                    if (hasSeller) {
                        scoped.add("MARKETPLACE_SELLER");
                        scoped.add("VENDOR");
                    }
                }
            }
        }

        return List.copyOf(scoped);
    }

    /**
     * Returns all communities the user belongs to along with their community-scoped roles
     * (e.g. Community A -> [OWNER, RESIDENT], Community B -> [TENANT, RESIDENT], Community C -> [VENDOR, SERVICE_PROVIDER]).
     */
    @Transactional(readOnly = true)
    public List<CommunityRoleContext> getCommunityContexts(AppUser user) {
        if (user == null) {
            return Collections.emptyList();
        }
        Long defaultCommunityId = resolveDefaultCommunityId(user);
        Map<Long, String> namesById = new LinkedHashMap<>();
        Map<Long, Set<String>> flatsByCommunity = new LinkedHashMap<>();

        if (user.getCommunity() != null && user.getCommunity().getId() != null) {
            namesById.put(user.getCommunity().getId(), user.getCommunity().getName());
        }
        if (user.getId() != null) {
            List<FlatMembership> memberships = flatMembershipRepository.findByUserId(user.getId());
            if (memberships != null) {
                for (FlatMembership fm : memberships) {
                    if (!"REVOKED".equalsIgnoreCase(fm.getAppAccessStatus())
                            && fm.getFlat() != null
                            && fm.getFlat().getCommunity() != null) {
                        Long cid = fm.getFlat().getCommunity().getId();
                        namesById.putIfAbsent(cid, fm.getFlat().getCommunity().getName());
                        if (fm.getFlat().getFlatNumber() != null) {
                            flatsByCommunity.computeIfAbsent(cid, k -> new LinkedHashSet<>())
                                    .add(fm.getFlat().getFlatNumber());
                        }
                    }
                }
            }
            if (serviceProviderRepository != null) {
                List<ServiceProvider> providers = serviceProviderRepository.findAllByUserId(user.getId());
                if (providers != null) {
                    for (ServiceProvider sp : providers) {
                        if (sp.getVerificationStatus() != VerificationStatus.REJECTED && sp.getCommunity() != null) {
                            namesById.putIfAbsent(sp.getCommunity().getId(), sp.getCommunity().getName());
                        }
                    }
                }
            }
            if (marketSellerRepository != null) {
                List<MarketSellerProfile> sellers = marketSellerRepository.findByUserIdAndIsActiveTrue(user.getId());
                if (sellers != null) {
                    for (MarketSellerProfile seller : sellers) {
                        if (seller.getCommunity() != null) {
                            namesById.putIfAbsent(seller.getCommunity().getId(), seller.getCommunity().getName());
                        }
                    }
                }
            }
        }

        List<CommunityRoleContext> contexts = new ArrayList<>();
        for (Map.Entry<Long, String> entry : namesById.entrySet()) {
            Long cid = entry.getKey();
            contexts.add(new CommunityRoleContext(
                    cid,
                    entry.getValue(),
                    resolveRolesForCommunity(user, cid),
                    List.copyOf(flatsByCommunity.getOrDefault(cid, Set.of())),
                    cid.equals(defaultCommunityId)
            ));
        }
        return List.copyOf(contexts);
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
